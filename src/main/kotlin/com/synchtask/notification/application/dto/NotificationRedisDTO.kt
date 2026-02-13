package com.synchtask.notification.application.dto

import com.synchtask.notification.domain.entity.NotificationType
import java.time.LocalDateTime

data class NotificationRedisDTO(
    val id: Long,
    val recipientEmail: String,
    val message: String,
    val createdAt: LocalDateTime,
    val type: NotificationType
)
