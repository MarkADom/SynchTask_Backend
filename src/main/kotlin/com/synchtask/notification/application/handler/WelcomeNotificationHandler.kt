package com.synchtask.notification.application.handler

import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class WelcomeNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {
    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "Welcome to SynchTask! We're excited to have you onboard.",
            NotificationType.SYSTEM
        )

        notificationService.sendNotification(
            user.email,
            "You can start by creating your first task or inviting friends.",
            NotificationType.SYSTEM
        )

        notificationService.sendNotification(
            user.email,
            "Check your dashboard for updates, tips and team activity.",
            NotificationType.SYSTEM
        )
    }

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.SYSTEM
    }
}
