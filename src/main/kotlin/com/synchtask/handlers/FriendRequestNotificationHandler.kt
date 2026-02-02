package com.synchtask.handlers

import com.synchtask.notification.application.handler.NotificationHandler
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.notification.application.service.NotificationService
import org.springframework.stereotype.Component

@Component
class FriendRequestNotificationHandler(
    private val notificationService: NotificationService
) : NotificationHandler {

    override fun supports(type: NotificationType): Boolean {
        return type == NotificationType.FRIEND_REQUEST
    }

    override fun handle(user: User) {
        notificationService.sendNotification(
            user.email,
            "You have received a new friend request.",
            NotificationType.FRIEND_REQUEST
        )
    }
}
