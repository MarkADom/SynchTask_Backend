package com.synchtask.notification.application.dto

import com.synchtask.notification.domain.entity.NotificationType

data class NotificationRequestDTO(
    val email: String,
    val message: String,
    val type: NotificationType,
    val groupId: Long? = null
)
