package com.synchtask.chat.application.dto

import java.time.LocalDateTime

data class ChatMessageDTO(
    val id: Long,
    val chatRoomId: Long,
    val senderEmail: String,
    val message: String,
    val timestamp: LocalDateTime
)
