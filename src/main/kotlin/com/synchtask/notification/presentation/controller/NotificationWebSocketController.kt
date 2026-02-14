package com.synchtask.notification.presentation.controller

import com.synchtask.notification.application.dto.NotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

/**
 * Internal WebSocket dispatcher for user notifications.
 *
 * Called from services, not exposed as a STOMP handler.
 */
@Controller
class NotificationWebSocketController(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(NotificationWebSocketController::class.java)

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
