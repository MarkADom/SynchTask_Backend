package com.synchtask.chat.application.dto

data class ChatRoomDTO(
    val id: Long,
    val participants: List<String>  // Emails of participants
)
