package com.synchtask.notification.application.handler

import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.notification.application.service.NotificationService
import org.springframework.stereotype.Component

/**
 * Dispatches notifications for incoming chat messages.
 */
@Component
class ChatMessageNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.CHAT_MESSAGE
    }

    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "You received a new chat message.",
            NotificationType.CHAT_MESSAGE
        )
    }
}
