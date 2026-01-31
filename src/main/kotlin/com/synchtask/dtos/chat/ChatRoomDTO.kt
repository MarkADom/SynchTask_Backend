package com.synchtask.dtos.chat

import com.synchtask.entities.ChatRoom

data class ChatRoomDTO(
    val id: Long,
    val participants: List<String>  // Emails of participants
) {
    companion object {

        fun fromEntity(chatRoom: ChatRoom): ChatRoomDTO {
            return ChatRoomDTO(
                id = chatRoom.id!!,
                participants = chatRoom.participants.map { it.email }
            )
        }
    }
}
