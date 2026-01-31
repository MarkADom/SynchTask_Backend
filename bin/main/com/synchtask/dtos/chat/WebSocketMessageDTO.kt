package com.synchtask.dtos.chat

import java.time.LocalDateTime

/**
 * **WebSocket Message DTO**
 *
 * Represents encrypted chat messages exchanged via WebSocket.
 */
data class WebSocketMessageDTO(
    val chatRoomId: Long,
    val senderEmail: String,
    val encryptedMessage: String, // The encrypted message
    val timestamp: LocalDateTime
)
