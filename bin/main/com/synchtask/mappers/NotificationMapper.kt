package com.synchtask.mappers

import com.synchtask.dtos.notification.NotificationDTO
import com.synchtask.dtos.notification.NotificationRedisDTO
import com.synchtask.dtos.notification.NotificationResponseDTO
import com.synchtask.entities.Notification

/**
 * **NotificationMapper**
 *
 * Converts `Notification` entities into various DTO formats for:
 * - **API responses** (`NotificationResponseDTO`)
 * - **WebSocket notifications** (`NotificationDTO`)
 * - **Redis caching** (`NotificationRedisDTO`)
 */
object NotificationMapper {

    /**
     * **Converts a `Notification` entity to `NotificationResponseDTO`**
     *
     * @param notification The notification entity.
     * @return A DTO for API responses.
     */
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

    /**
     * **Converts a `Notification` entity to `NotificationDTO` for WebSocket messages**
     *
     * @param notification The notification entity.
     * @return A DTO for WebSocket events.
     */
    fun toWebSocketDTO(notification: Notification): NotificationDTO {
        return NotificationDTO(
            id = notification.id ?: throw IllegalArgumentException("Notification ID cannot be null"),
            recipientEmail = notification.recipient.email,
            message = notification.message,
            timestamp = notification.createdAt,
            type = notification.type
        )
    }

    /**
     * **Converts a `Notification` entity to `NotificationRedisDTO` for Redis storage**
     *
     * @param notification The notification entity.
     * @return A DTO optimized for Redis.
     */
    fun toRedisDTO(notification: Notification): NotificationRedisDTO {
        return NotificationRedisDTO(
            id = notification.id ?: throw IllegalArgumentException("Notification ID cannot be null"),
            recipientEmail = notification.recipient.email,
            message = notification.message,
            createdAt = notification.createdAt,
            type = notification.type
        )
    }

    /**
     * **Converts a `NotificationRedisDTO` to `NotificationResponseDTO`**
     *
     * This allows retrieving cached notifications.
     */
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
