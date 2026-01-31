package com.synchtask.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * **Swagger Configuration**
 *
 * - Enables OpenAPI documentation.
 * - Adds JWT Authentication Support.
 */
@Configuration
@OpenAPIDefinition(
    info = Info(title = "SynchTask API", version = "1.0", description = "API Documentation for SynchTask"),
    security = [SecurityRequirement(name = "BearerAuth")]
)

@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)

class SwaggerConfig {
    /**
     * Groups all APIs under "synchtask".
     */
    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("synchtask")
            .pathsToMatch("/**")
            .build()
    }
}

