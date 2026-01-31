package com.synchtask.dtos.chat

import com.synchtask.entities.ChatRoom

/**
 * **Chat Room DTO**
 *
 * Represents a chat room.
 */
data class ChatRoomDTO(
    val id: Long,
    val participants: List<String>  // Emails of participants
) {
    companion object {
        /**
         * Converts a `ChatRoom` entity to a `ChatRoomDTO`.
         */
        fun fromEntity(chatRoom: ChatRoom): ChatRoomDTO {
            return ChatRoomDTO(
                id = chatRoom.id!!,
                participants = chatRoom.participants.map { it.email }
            )
        }
    }
}
