package com.synchtask.websocket

import com.synchtask.handlers.WebSocketReconnectionHandler
import com.synchtask.user.application.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.concurrent.CompletableFuture

@Component
class WebSocketEventListener(
    private val userService: UserService,
    private val webSocketReconnectionHandler: WebSocketReconnectionHandler
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent) {
        val userEmail = StompHeaderAccessor.wrap(event.message).user?.name

        if (userEmail.isNullOrBlank()) {
            logger.warn("[WebSocket] Connection event without user identity.")
            return
        }

        logger.info("[WebSocket] User connected: $userEmail")

        CompletableFuture.runAsync {
            userService.setUserOnlineStatus(userEmail, isOnline = true)
        }
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val userEmail = StompHeaderAccessor.wrap(event.message).user?.name

        if (userEmail.isNullOrBlank()) {
            logger.warn("[WebSocket] Disconnect event without user identity.")
            return
        }

        logger.info("[WebSocket] User disconnected: $userEmail")

        CompletableFuture.runAsync {
            userService.setUserOnlineStatus(userEmail, isOnline = false)

            if (webSocketReconnectionHandler.shouldAttemptReconnection(userEmail)) {
                logger.debug("[WebSocket] Attempting reconnection handling for: $userEmail")
                webSocketReconnectionHandler.handleDisconnect(event)
            }
        }
    }
}
