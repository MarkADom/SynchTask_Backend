package com.synchtask.notification.application.handler

import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.notification.application.service.NotificationService
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
