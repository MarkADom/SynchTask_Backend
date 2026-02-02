package com.synchtask.notification.application.handler

import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.notification.application.service.NotificationService
import org.springframework.stereotype.Component

@Component
class SystemNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.SYSTEM
    }

    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "Welcome to SynchTask! Here are your first steps.",
            NotificationType.SYSTEM
        )

        notificationService.sendNotification(
            user.email,
            "Create your first board or join a team to get started!",
            NotificationType.SYSTEM
        )
    }
}
