package com.synchtask.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * WebSocket-related settings loaded from configuration.
 */
@ConditionalOnProperty(
    name = ["websocket.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Configuration
@ConfigurationProperties(prefix = "websocket")
class WebSocketProperties(
    val allowedOrigins: List<String> = emptyList(),
)
