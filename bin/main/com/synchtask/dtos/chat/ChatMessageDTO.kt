package com.synchtask.dtos.chat

import com.synchtask.entities.ChatMessage
import java.time.LocalDateTime

/**
 * Represents a chat message sent between users.
 */
data class ChatMessageDTO(
    val id: Long,
    val chatRoomId: Long,
    val senderEmail: String,
    val message: String,
    val timestamp: LocalDateTime
) {
    companion object {
        /**
         * Converts a ChatMessage entity to its DTO representation.
         *
         * @param entity The ChatMessage entity to convert.
         * @return A ChatMessageDTO instance.
         */
        fun fromEntity(entity: ChatMessage) = ChatMessageDTO(
            id = entity.id ?: error("ChatMessage ID cannot be null"),
            chatRoomId = entity.chatRoom.id ?: error("ChatRoom ID cannot be null"),
            senderEmail = entity.sender.email,
            message = entity.encryptedMessage,
            timestamp = entity.timestamp
        )
    }
}
