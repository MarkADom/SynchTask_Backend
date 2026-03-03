# SynchTask Backend

![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Gradle](https://img.shields.io/badge/Gradle-Build-blueviolet)
![JWT](https://img.shields.io/badge/Auth-JWT%20RS256-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

SynchTask Backend is a production-ready collaborative task management API built with Kotlin (JVM), Spring Boot 3 and Clean Architecture principles.
It serves as the backend core of the SynchTask ecosystem and is developed as a portfolio-grade engineering project.

## Scope (v1.0)

- Monolithic backend (no microservices)
- Stateless JWT security (RS256 + JWKS)
- Hybrid authorization (global roles + membership-based access)
- Validation-hardened HTTP contract
- Backend-only repository (frontend under development)

## Tech stack

- Kotlin + Spring Boot 3
- MySQL + Redis
- JWT/OAuth2 security
- OpenAPI / Swagger UI
- Bruno integration test suites

## Run locally

1. Configure environment variables:

```bash
cp .envrc.example .envrc
# edit values for your local environment
```

If you use direnv:

```bash
direnv allow
```

2. Run the application:

```bash
./gradlew bootRun
```

3. Run with dev profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

4. Open API docs:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Testing & quality

```bash
./gradlew test
./gradlew detekt
./gradlew jacocoTestReport
```

## Documentation map

- [Architecture](docs/architecture.md)
- [Configuration](docs/configuration.md)
- [HTTP Error Semantics](docs/http-error-semantics.md)
- [Database](docs/database/)
- [Engineering Decisions (v1.0)](docs/engineering_decisions_v1.0.md)
- [OpenAPI Export](docs/openapi/api-docs.json)
- [Bruno Test Suites](bruno/README.md)

## License

Licensed under the terms in [`LICENSE.md`](LICENSE.md).
