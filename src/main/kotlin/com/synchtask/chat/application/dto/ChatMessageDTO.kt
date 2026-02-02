package com.synchtask.chat.application.dto

import com.synchtask.chat.domain.entity.ChatMessage
import java.time.LocalDateTime

data class ChatMessageDTO(
    val id: Long,
    val chatRoomId: Long,
    val senderEmail: String,
    val message: String,
    val timestamp: LocalDateTime
) {
    companion object {

        fun fromEntity(entity: ChatMessage) = ChatMessageDTO(
            id = entity.id ?: error("ChatMessage ID cannot be null"),
            chatRoomId = entity.chatRoom.id ?: error("ChatRoom ID cannot be null"),
            senderEmail = entity.sender.email,
            message = entity.encryptedMessage,
            timestamp = entity.timestamp
        )
    }
}
