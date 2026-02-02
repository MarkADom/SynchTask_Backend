package com.synchtask.dtos.notification

import com.synchtask.entities.Notification
import com.synchtask.entities.NotificationType
import com.synchtask.user.domain.entity.User
import java.time.LocalDateTime

data class NotificationRedisDTO(
    val id: Long,
    val recipientEmail: String,
    val message: String,
    val createdAt: LocalDateTime,
    val type: NotificationType
) {

    fun toNotificationEntity(recipient: User): Notification {
        return Notification(
            id = id,
            recipient = recipient,
            message = message,
            createdAt = createdAt,
            type = type
        )
    }

    companion object {
        fun fromEntity(notification: Notification): NotificationRedisDTO {
            return NotificationRedisDTO(
                id = notification.id ?: throw IllegalArgumentException("Notification ID cannot be null"),
                recipientEmail = notification.recipient.email,
                message = notification.message,
                createdAt = notification.createdAt,
                type = notification.type
            )
        }
    }
}
