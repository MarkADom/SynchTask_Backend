package com.synchtask.notification.presentation.mapper

import com.synchtask.notification.application.dto.NotificationDTO
import com.synchtask.notification.application.dto.NotificationRedisDTO
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.Notification

object NotificationMapper {

    fun toResponseDTO(notification: Notification): NotificationResponseDTO {
        return NotificationResponseDTO(
            id = notification.id ?: throw IllegalArgumentException("Notification ID cannot be null"),
            recipientEmail = notification.recipient.email,
            message = notification.message,
            isRead = notification.isRead,
            createdAt = notification.createdAt,
            type = notification.type,
            groupId = notification.groupId
        )
    }

    fun toWebSocketDTO(notification: Notification): NotificationDTO {
        return NotificationDTO(
            id = notification.id ?: throw IllegalArgumentException("Notification ID cannot be null"),
            recipientEmail = notification.recipient.email,
            message = notification.message,
            timestamp = notification.createdAt,
            type = notification.type
        )
    }

    fun toRedisDTO(notification: Notification): NotificationRedisDTO {
        return NotificationRedisDTO(
            id = notification.id ?: throw IllegalArgumentException("Notification ID cannot be null"),
            recipientEmail = notification.recipient.email,
            message = notification.message,
            createdAt = notification.createdAt,
            type = notification.type
        )
    }

    fun fromRedisDTO(notification: NotificationRedisDTO): NotificationResponseDTO {
        return NotificationResponseDTO(
            id = notification.id,
            recipientEmail = notification.recipientEmail,
            message = notification.message,
            isRead = false, // Redis notifications are assumed unread
            createdAt = notification.createdAt,
            type = notification.type,
            groupId = null // Redis does not store groupId
        )
    }
}
