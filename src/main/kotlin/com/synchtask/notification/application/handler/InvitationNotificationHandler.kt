package com.synchtask.notification.application.handler

import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.notification.application.service.NotificationService
import org.springframework.stereotype.Component

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
