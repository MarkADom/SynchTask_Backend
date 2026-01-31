package com.synchtask.websocket

import com.synchtask.handlers.WebSocketReconnectionHandler
import com.synchtask.services.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.concurrent.CompletableFuture

/**
 * WebSocket Event Listener
 *
 * Listens to WebSocket session events and updates user presence state accordingly.
 * Also delegates disconnection handling logic for potential reconnection support.
 */
@Component
class WebSocketEventListener(
    private val userService: UserService,
    private val webSocketReconnectionHandler: WebSocketReconnectionHandler
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    /**
     * Handles WebSocket session connect events.
     *
     * Marks the user as online in the system asynchronously.
     */
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

    /**
     * Handles WebSocket session disconnect events.
     *
     * Marks the user as offline and triggers reconnection logic if necessary.
     */
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
