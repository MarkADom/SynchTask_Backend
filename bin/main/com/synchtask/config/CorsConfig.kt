package com.synchtask.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Global CORS Configuration.
 *
 * Dynamically applies CORS based on active environment.
 * - In `dev` or `local` profile, allows all origins.
 * - Otherwise, uses the `application.yml` whitelist.
 */
@Configuration
@ConfigurationProperties(prefix = "cors")
class CorsConfig(
    private val environment: Environment,
) : WebMvcConfigurer {

    lateinit var allowed: Allowed

    override fun addCorsMappings(registry: CorsRegistry) {
        val isDev = environment.activeProfiles.any { it == "dev" || it == "local" }

        val mapping = registry.addMapping("/**")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin"
            )
            .exposedHeaders(
                "Authorization",
                "X-Frame-Options",
                "Access-Control-Allow-Origin"
            )
            .allowCredentials(true)

        if (isDev) {
            logger.warn("CORS is configured to allow all origin patterns in development.")
            mapping.allowedOriginPatterns("*")
        } else {
            mapping.allowedOrigins(*allowed.origins.toTypedArray())
        }
    }

    data class Allowed(var origins: List<String> = listOf())

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(CorsConfig::class.java)
    }
}
