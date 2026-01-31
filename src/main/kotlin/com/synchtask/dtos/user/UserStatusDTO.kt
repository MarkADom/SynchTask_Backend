package com.synchtask.dtos.user

import java.time.LocalDateTime

data class UserStatusDTO(
    val email: String,
    val name: String,
    val lastActivity: LocalDateTime
)
