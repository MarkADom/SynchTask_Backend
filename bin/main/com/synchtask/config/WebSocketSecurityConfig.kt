package com.synchtask.config

import com.synchtask.security.JwtTokenProvider
import com.synchtask.websocket.CustomHandshakeInterceptor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket Configuration
 *
 * - Configures native STOMP WebSocket endpoints (/ws, /ws-notifications)
 * - Uses JWT authentication via CustomHandshakeInterceptor
 * - Applies dynamic CORS policy loaded from application.yml
 * - Avoids SockJS for modern, scalable WebSocket infrastructure
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(name = ["websocket.enabled"], havingValue = "true", matchIfMissing = true)
class WebSocketSecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val environment: Environment,
    private val corsProperties: CorsProperties
) : WebSocketMessageBrokerConfigurer {

    private val logger = LoggerFactory.getLogger(WebSocketSecurityConfig::class.java)

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = corsProperties.origins.toTypedArray()

        // Native WebSocket for main features
        registry.addEndpoint("/ws")
            .setAllowedOrigins(*origins)
            .addInterceptors(customHandshakeInterceptor())

        // Separate endpoint for notification channel
        registry.addEndpoint("/ws-notifications")
            .setAllowedOrigins(*origins)
            .addInterceptors(customHandshakeInterceptor())

        // Development/Test only endpoint
        if (environment.activeProfiles.contains("test")) {
            registry.addEndpoint("/ws-test")
                .setAllowedOrigins(*origins)
        }

        logger.info("Registered STOMP WebSocket endpoints: /ws, /ws-notifications")
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        logger.info("Configuring STOMP broker prefixes")
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    @Bean
    fun customHandshakeInterceptor(): CustomHandshakeInterceptor {
        return CustomHandshakeInterceptor(jwtTokenProvider)
    }

    @Bean
    fun webSocketAuthorizationManager(): AuthorizationManager<Message<*>> {
        return MessageMatcherDelegatingAuthorizationManager.builder()
            .simpTypeMatchers(SimpMessageType.CONNECT).permitAll() // TODO: change to authenticated() in production
            .simpDestMatchers("/ws/**", "/ws-notifications/**").authenticated()
            .simpDestMatchers("/user/queue/**").authenticated()
            .simpDestMatchers("/topic/**").permitAll()
            .anyMessage().denyAll()
            .build()
    }
}
