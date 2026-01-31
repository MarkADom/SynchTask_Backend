package com.synchtask.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Holds the configured CORS origins.
 *
 * Kept as a small helper to avoid duplicating parsing logic
 * across REST and WebSocket configuration.
 */
@Component
@ConfigurationProperties(prefix = "cors.allowed")
class CorsProperties {

    var origins: List<String> = listOf()

    fun getResolvedOrigins(): List<String> {
        // Handle both YAML lists and comma-separated strings
        if (origins.size == 1 && origins.first().contains(",")) {
            return origins.first().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        return origins
    }

}
