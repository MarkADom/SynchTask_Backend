package com.synchtask.dtos.user

import java.time.LocalDateTime

/**
 * **User Status DTO**
 *
 * Represents online users with their last activity timestamp.
 */
data class UserStatusDTO(
    val email: String,
    val name: String,
    val lastActivity: LocalDateTime
)
