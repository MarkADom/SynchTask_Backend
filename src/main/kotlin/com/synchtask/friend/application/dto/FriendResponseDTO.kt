package com.synchtask.friend.application.dto


data class FriendResponseDTO(
    val id: Long,
    val friendEmail: String,
    val requesterEmail: String,
    val status: String,
    val isIncoming: Boolean
)
