package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.services.notification.NotificationService
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
