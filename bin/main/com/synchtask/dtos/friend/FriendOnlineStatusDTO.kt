package com.synchtask.dtos.friend

import java.time.LocalDateTime

/**
 * **Friend Online Status DTO**
 *
 * Represents the real-time notification of a friend's online status.
 */
data class FriendOnlineStatusDTO(
    val userEmail: String,
    val isOnline: Boolean,
    val timestamp: LocalDateTime
)
