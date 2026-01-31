package com.synchtask.dtos.chat

import java.time.LocalDateTime

data class WebSocketMessageDTO(
    val chatRoomId: Long,
    val senderEmail: String,
    val encryptedMessage: String, // The encrypted message
    val timestamp: LocalDateTime
)
