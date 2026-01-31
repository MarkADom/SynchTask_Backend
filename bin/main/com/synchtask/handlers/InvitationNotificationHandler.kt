package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.services.notification.NotificationService
import org.springframework.stereotype.Component

/**
 * Handles invitation notifications.
 */
@Component
class InvitationNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.INVITATION
    }

    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "You've been invited to collaborate on a board!",
            NotificationType.INVITATION
        )
    }
}
