package com.synchtask.notification.application.dto

import com.synchtask.notification.domain.entity.NotificationType

data class NotificationRequestDTO(
    val email: String,
    val message: String,
    val type: NotificationType, // Define the type of notification
    val groupId: Long? = null // Optional: Required for group notifications
)
