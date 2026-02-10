package com.synchtask.notification.realtime

import com.synchtask.chat.application.dto.ChatNotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

/**
 * Real-time chat notifications.
 *
 * This service is deliberately excluded from the Activity / NotificationPolicy pipeline.
 * Do NOT move chat events into the activity domain.
 *
 * Chat events are transient, real-time, and non-auditable by design.
 */

@Service
class ChatNotificationService(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(ChatNotificationService::class.java)

    fun notifyUser(notification: ChatNotificationDTO) {
        messagingTemplate.convertAndSend("/queue/notifications/${notification.recipientEmail}", notification)
        logger.info("Sent notification to user: ${notification.recipientEmail} for chatRoom ${notification.chatRoomId}")
    }
}
