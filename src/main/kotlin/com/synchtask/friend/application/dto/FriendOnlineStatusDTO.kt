package com.synchtask.friend.application.dto

import java.time.LocalDateTime

data class FriendOnlineStatusDTO(
    val userEmail: String,
    val isOnline: Boolean,
    val timestamp: LocalDateTime
)
