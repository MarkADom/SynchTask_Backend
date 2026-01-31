package com.synchtask.services.notification

import com.synchtask.dtos.chat.ChatNotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

/**
 * **Chat Notification Service**
 *
 * Sends real-time chat notifications to users when they receive a new message.
 */
@Service
class ChatNotificationService(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(ChatNotificationService::class.java)

    /**
     * Sends a real-time notification to the recipient of a new chat message.
     *
     * @param notification The notification data to be sent.
     */
    fun notifyUser(notification: ChatNotificationDTO) {
        messagingTemplate.convertAndSend("/queue/notifications/${notification.recipientEmail}", notification)
        logger.info("Sent notification to user: ${notification.recipientEmail} for chatRoom ${notification.chatRoomId}")
    }
}
