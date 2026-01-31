package com.synchtask.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration properties for CORS origins.
 *
 * This class allows reading a list of allowed origins from `application.yml`
 * under the key `cors.allowed.origins`, to be reused in REST and WebSocket layers.
 */
@Component
@ConfigurationProperties(prefix = "cors.allowed")
class CorsProperties {
    var origins: List<String> = listOf()
}
