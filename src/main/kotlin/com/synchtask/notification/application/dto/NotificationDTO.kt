package com.synchtask.notification.application.dto

import com.synchtask.notification.domain.entity.NotificationType
import java.time.LocalDateTime

data class NotificationDTO(
    val id: Long? = null,
    val recipientEmail: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val read: Boolean = false,
    val type: NotificationType
)
