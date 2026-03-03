package com.synchtask.notification.presentation.mapper

import com.synchtask.notification.application.dto.NotificationDTO
import com.synchtask.notification.application.dto.NotificationRedisDTO
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.Notification
import com.synchtask.shared.presentation.mapper.MapperSupport.requireId

object NotificationMapper {
    fun toResponseDTO(notification: Notification): NotificationResponseDTO = NotificationResponseDTO(
        id = requireId(notification.id, "Notification"),
        recipientEmail = notification.recipient.email,
        message = notification.message,
        isRead = notification.isRead,
        createdAt = notification.createdAt,
        type = notification.type,
        groupId = notification.groupId
    )

    fun toWebSocketDTO(notification: Notification): NotificationDTO = NotificationDTO(
        id = requireId(notification.id, "Notification"),
        recipientEmail = notification.recipient.email,
        message = notification.message,
        timestamp = notification.createdAt,
        type = notification.type
    )

    fun toRedisDTO(notification: Notification): NotificationRedisDTO = NotificationRedisDTO(
        id = requireId(notification.id, "Notification"),
        recipientEmail = notification.recipient.email,
        message = notification.message,
        createdAt = notification.createdAt,
        type = notification.type
    )

    fun fromRedisDTO(notification: NotificationRedisDTO): NotificationResponseDTO = NotificationResponseDTO(
        id = notification.id,
        recipientEmail = notification.recipientEmail,
        message = notification.message,
        isRead = false,
        createdAt = notification.createdAt,
        type = notification.type,
        groupId = null
    )
}
