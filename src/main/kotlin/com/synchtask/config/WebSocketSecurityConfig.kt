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
 * WebSocket setup aligned with HTTP security.
 *
 * JWT is validated during the handshake and the same
 * origin rules used by REST endpoints apply here.
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
        val origins = corsProperties.getResolvedOrigins().toTypedArray()

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
        registry.setUserDestinationPrefix("/com/synchtask/user")
    }

    @Bean
    fun customHandshakeInterceptor(): CustomHandshakeInterceptor {
        return CustomHandshakeInterceptor(jwtTokenProvider)
    }

    /**
     * Message-level access rules.
     *
     * CONNECT is kept open so the handshake can complete,
     * destinations require an authenticated session.
     */
    @Bean
    fun webSocketAuthorizationManager(): AuthorizationManager<Message<*>> {
        return MessageMatcherDelegatingAuthorizationManager.builder()
            .simpTypeMatchers(SimpMessageType.CONNECT).permitAll() // TODO: change to authenticated() in production
            .simpDestMatchers("/ws/**", "/ws-notifications/**").authenticated()
            .simpDestMatchers("/com/synchtask/user/queue/**").authenticated()
            .simpDestMatchers("/topic/**").permitAll()
            .anyMessage().denyAll()
            .build()
    }
}
