package com.synchtask.dtos.notification

import com.synchtask.entities.NotificationType
import java.time.LocalDateTime

/**
 * **NotificationResponseDTO**
 *
 * Data Transfer Object (DTO) for notifications.
 *
 * - Contains notification details such as `id`, `recipientEmail`, `message`, and `read` status.
 * - Includes `type` to categorize notifications.
 * - If the notification is related to a group, the `groupId` field stores the group reference.
 */
data class NotificationResponseDTO(
    val id: Long?,
    val recipientEmail: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val type: NotificationType,  // Notification type: PERSONAL, GROUP, or SYSTEM
    val groupId: Long? = null // Group ID for group notifications
)
