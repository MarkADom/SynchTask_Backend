package com.synchtask.notification.application.dto

import com.synchtask.notification.domain.entity.NotificationType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class NotificationRequestDTO(
    @field:NotBlank(message = "Recipient email is required")
    @field:Email(message = "Recipient email should be valid")
    val email: String,
    @field:NotBlank(message = "Notification message is required")
    @field:Size(min = 1, max = 1000, message = "Notification message must be between 1 and 1000 characters")
    val message: String,
    val type: NotificationType,
    @field:Positive(message = "Group id must be positive")
    val groupId: Long? = null,
)
