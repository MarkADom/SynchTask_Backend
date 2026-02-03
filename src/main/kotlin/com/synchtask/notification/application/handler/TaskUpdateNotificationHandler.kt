package com.synchtask.notification.application.handler

import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.notification.application.service.NotificationService
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
