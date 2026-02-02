package com.synchtask.notification.application.service

import com.synchtask.notification.application.dto.NotificationDTO
import com.synchtask.managers.WebSocketManager
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class NotificationWebSocketService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val webSocketManager: WebSocketManager,
    private val notificationStorageService: NotificationStorageService
) {
    private val logger = LoggerFactory.getLogger(NotificationWebSocketService::class.java)

    fun sendNotification(userEmail: String, notification: NotificationDTO) {
        if (webSocketManager.isUserOnline(userEmail)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification)
            logger.info("WebSocket notification sent to $userEmail")
        } else {
            notificationStorageService.storeNotification(
                recipientEmail = userEmail,
                message = notification.message,
                type = notification.type
            )
            logger.warn("User $userEmail is offline. Notification stored for later delivery.")
        }
    }
}
