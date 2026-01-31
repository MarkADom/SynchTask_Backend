package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.services.notification.NotificationService
import org.springframework.stereotype.Component

@Component
class GroupNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.GROUP
    }

    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "You’ve been added to a new group or project.",
            NotificationType.GROUP
        )
    }
}
