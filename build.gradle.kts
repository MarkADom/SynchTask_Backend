import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // ───── Code Quality ─────
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    id("org.sonarqube") version "5.0.0.4638"
    id("jacoco")

    // ───── Spring Boot ─────
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.4"

    // ───── Kotlin ─────
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}
group = "com.synchtask"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

jacoco {
    toolVersion = "0.8.10"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt.yml"))
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {

    // ───── Spring Boot Core ─────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    // ───── Security & Auth ─────
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-client")

    // ───── Rate Limiting ─────
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

    // ───── Database & Persistence ─────
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    // ───── Redis (Cache & Pub/Sub) ─────
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.apache.commons:commons-pool2:2.11.1")
    implementation("io.projectreactor.netty:reactor-netty:1.0.40")

    // ───── JSON & Serialization ─────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ───── JWT ─────
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // ───── Validation ─────
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ───── API Documentation ─────
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // ───── Dev Tools ─────
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ───── Testing ─────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

// ───── Kotlin Compiler ─────
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs =
            listOf(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn"
            )
        jvmTarget = "21"
    }
}

// ───── Tests ─────
tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

// ───── JaCoCo ─────
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    }

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/main")) {
            exclude(
                "**/dto/**",
                "**/exception/**"
            )
        }
    )

    sourceDirectories.setFrom(files("src/main/kotlin"))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/test.exec")
        }
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// ───── SpringBoot ─────
tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    mainClass.set("com.synchtask.SynchTaskApplicationKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.synchtask.SynchTaskApplicationKt"
    }
}

// SonarQube / SonarCloud Configuration

val sonarHostUrl = System.getenv("SONAR_HOST_URL")
val sonarToken = System.getenv("SONAR_TOKEN")
val isCI = System.getenv("CI") == "true"

val effectiveSonarHost = sonarHostUrl ?: "https://sonarcloud.io"
val isSonarCloud = effectiveSonarHost.contains("sonarcloud.io")

tasks.named("sonar") {
    dependsOn("test", "jacocoTestReport", "detekt")

    doFirst {
        if (sonarToken.isNullOrBlank()) {
            throw GradleException("SONAR_TOKEN must be defined to run Sonar analysis.")
        }
    }
}

sonar {
    properties {

        property("sonar.host.url", effectiveSonarHost)
        property("sonar.token", sonarToken)

        if (isSonarCloud) {
            property("sonar.organization", "markadom")
            property("sonar.projectKey", "MarkADom_SynchTask_Backend")
            property("sonar.projectName", "SynchTask_Backend")
        } else {
            property("sonar.projectKey", "com.synchtask:backend")
            property("sonar.projectName", "SynchTask")
        }

        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")

        property(
            "sonar.kotlin.detekt.reportPaths",
            layout.buildDirectory
                .file("reports/detekt/detekt.xml")
                .get()
                .asFile
                .absolutePath
        )

        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory
                .file("reports/jacoco/test/jacocoTestReport.xml")
                .get()
                .asFile
                .absolutePath
        )
    }
}

