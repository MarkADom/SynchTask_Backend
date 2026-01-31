package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.services.notification.NotificationService
import org.springframework.stereotype.Component

@Component
class TaskUpdateNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.TASK_UPDATE
    }

    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "A task assigned to you has been updated.",
            NotificationType.TASK_UPDATE
        )
    }
}
