package com.synchtask.services.notification

import com.synchtask.chat.application.dto.ChatNotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

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
