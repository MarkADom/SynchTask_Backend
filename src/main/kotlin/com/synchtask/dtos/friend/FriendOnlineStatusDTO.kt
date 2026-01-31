package com.synchtask.dtos.friend

import java.time.LocalDateTime

data class FriendOnlineStatusDTO(
    val userEmail: String,
    val isOnline: Boolean,
    val timestamp: LocalDateTime
)
