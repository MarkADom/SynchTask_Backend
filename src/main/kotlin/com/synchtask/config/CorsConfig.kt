package com.synchtask.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Central CORS configuration.
 *
 * Dev/local environments are intentionally permissive.
 * Other environments rely on an explicit whitelist.
 */
@Configuration
@ConfigurationProperties(prefix = "cors.allowed")
class CorsConfig(
    private val environment: Environment,
) : WebMvcConfigurer {

    var origins: List<String> = listOf()

    override fun addCorsMappings(registry: CorsRegistry) {
        val isDev = environment.activeProfiles.any { it == "dev" || it == "local" }


        val mapping = registry.addMapping("/**")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin")
            .exposedHeaders("Authorization", "X-Frame-Options", "Access-Control-Allow-Origin")
            .allowCredentials(true)

        if (isDev) {
            logger.warn("CORS running in dev/local mode — allowing all origins.")
            mapping.allowedOriginPatterns("*")
        } else {
            require(origins.isNotEmpty()) {
                "CORS is enabled but no allowed origins are configured."
            }

            logger.info("CORS allowed origins: ${origins.joinToString()}")
            mapping.allowedOrigins(*origins.toTypedArray())
        }
    }

    data class Allowed(var origins: List<String> = listOf())

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(CorsConfig::class.java)
    }
}
