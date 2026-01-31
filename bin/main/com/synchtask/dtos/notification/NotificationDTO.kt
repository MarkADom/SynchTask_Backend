package com.synchtask.dtos.notification

import com.synchtask.entities.NotificationType
import java.time.LocalDateTime

/**
 * **Notification DTO**
 *
 * Represents a notification sent to the user.
 */
data class NotificationDTO(
    val id: Long? = null,
    val recipientEmail: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val read: Boolean = false,
    val type: NotificationType
)
