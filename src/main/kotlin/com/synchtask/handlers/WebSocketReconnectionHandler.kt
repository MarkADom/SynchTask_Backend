package com.synchtask.handlers

import com.synchtask.managers.WebSocketManager
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.io.IOException
import java.util.concurrent.*
import kotlin.math.pow

@Component
class WebSocketReconnectionHandler(
    private val webSocketManager: WebSocketManager,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(WebSocketReconnectionHandler::class.java)
    private val activeSessions = ConcurrentHashMap<String, String>() // sessionId -> userEmail
    private val scheduler = Executors.newScheduledThreadPool(1) // Scheduler for reconnection attempts

    fun handleConnect(event: SessionConnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val userEmail = accessor.user?.name ?: return

        // Prevent multiple active sessions for the same user
        activeSessions.values.remove(userEmail) // Remove any existing session for this user
        activeSessions[sessionId] = userEmail

        logger.info("WebSocket connected: User=$userEmail, Session=$sessionId")
    }

    fun handleDisconnect(event: SessionDisconnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val userEmail = activeSessions.remove(sessionId) ?: return

        logger.warn("WebSocket disconnected: User=$userEmail, Session=$sessionId. Attempting reconnection...")

        scheduleReconnection(userEmail, 1)
    }

    private fun scheduleReconnection(userEmail: String, attempt: Int) {
        // Check if user is already online, cancel reconnection if true
        if (webSocketManager.isUserOnline(userEmail)) {
            logger.info("User $userEmail is already online. Skipping reconnection.")
            return
        }

        if (attempt > MAX_RETRIES) {
            logger.error("WebSocket reconnection failed after $MAX_RETRIES attempts for user: $userEmail")
            return
        }

        val delay = calculateBackoffDelay(attempt)
        scheduler.schedule({
            try {
                logger.info("Attempting WebSocket reconnection for $userEmail (Attempt $attempt)...")

                if (!webSocketManager.isUserOnline(userEmail)) {
                    messagingTemplate.convertAndSendToUser(userEmail, "/queue/reconnect", "RECONNECT")
                } else {
                    logger.info("User $userEmail reconnected before retry. Stopping reconnection attempts.")
                }

            } catch (ex: InterruptedException) {
                logger.warn("WebSocket reconnection interrupted for $userEmail: ${ex.message}", ex)
                Thread.currentThread().interrupt()

            } catch (ex: IllegalStateException) {
                logger.error("Invalid WebSocket reconnection state for $userEmail: ${ex.message}", ex)

            } catch (ex: TimeoutException) {
                logger.warn("WebSocket reconnection timeout for $userEmail (Attempt $attempt)", ex)

            } catch (ex: IOException) {
                logger.error("WebSocket error during reconnection for $userEmail: ${ex.message}", ex)

            } finally {
                // ✅ Only schedule the next retry if user is still offline
                if (!webSocketManager.isUserOnline(userEmail)) {
                    scheduleReconnection(userEmail, attempt + 1)
                }
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        return (BASE_BACKOFF_DELAY_MS * BACKOFF_MULTIPLIER.pow(attempt.toDouble())).toLong()
    }

    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    fun shouldAttemptReconnection(userEmail: String): Boolean {
        return webSocketManager.isUserOnline(userEmail)
    }

    companion object {
        private const val SHUTDOWN_TIMEOUT_SECONDS = 5L
        private const val MAX_RETRIES = 5
        private const val BASE_BACKOFF_DELAY_MS = 2000L
        private const val BACKOFF_MULTIPLIER = 2.0
    }
}
