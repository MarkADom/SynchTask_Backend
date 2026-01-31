package com.synchtask.dtos.notification

import com.synchtask.entities.NotificationType
import java.time.LocalDateTime

data class NotificationResponseDTO(
    val id: Long?,
    val recipientEmail: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val type: NotificationType,  // Notification type: PERSONAL, GROUP, or SYSTEM
    val groupId: Long? = null // Group ID for group notifications
)
