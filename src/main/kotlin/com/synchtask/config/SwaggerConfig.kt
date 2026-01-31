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
 * OpenAPI configuration for the HTTP API.
 *
 * Keeps Swagger aligned with JWT-based security.
 */
@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "SynchTask API",
        version = "1.0",
        description = "API Documentation for SynchTask"),
    security = [SecurityRequirement(name = "BearerAuth")]
)

@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)

class SwaggerConfig {

    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("synchtask")
            .pathsToMatch("/**")
            .build()
    }
}

