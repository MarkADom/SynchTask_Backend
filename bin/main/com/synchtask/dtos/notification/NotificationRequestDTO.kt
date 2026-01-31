package com.synchtask.dtos.notification

import com.synchtask.entities.NotificationType

/**
 * **Notification Request DTO**
 *
 * Represents a request to send a notification.
 *
 * @property email The recipient's email.
 * @property message The notification message.
 * @property type The type of notification (`PERSONAL`, `GROUP`, `SYSTEM`).
 * @property groupId (Optional) The ID of the group if it's a group-related notification.
 */
data class NotificationRequestDTO(
    val email: String,
    val message: String,
    val type: NotificationType,  // Define the type of notification
    val groupId: Long? = null  // Optional: Required for group notifications
)
