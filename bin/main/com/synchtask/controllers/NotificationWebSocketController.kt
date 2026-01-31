package com.synchtask.controllers

import com.synchtask.dtos.notification.NotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

/**
 * **Notification WebSocket Controller**
 *
 * Handles dispatching real-time notifications to users via STOMP WebSockets.
 */
@Controller
class NotificationWebSocketController(
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(NotificationWebSocketController::class.java)

    /**
     * **Sends a WebSocket notification to a specific user.**
     *
     * @param userEmail Target user email.
     * @param notification The notification payload.
     *
     * This method should be called from backend services,
     * not exposed directly as an endpoint or STOMP handler.
     */
    fun notifyUser(userEmail: String, notification: NotificationDTO) {
        val authenticatedEmail = SecurityContextHolder.getContext().authentication?.name

        if (authenticatedEmail != userEmail) {
            logger.warn("Unauthorized WebSocket attempt: $authenticatedEmail tried to notify $userEmail")
            return
        }

        logger.info("Sending WebSocket notification to $userEmail")
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification)
    }
}
