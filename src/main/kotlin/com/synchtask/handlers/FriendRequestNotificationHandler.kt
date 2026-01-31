package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.services.notification.NotificationService
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
