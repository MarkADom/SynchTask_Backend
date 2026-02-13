package com.synchtask.chat.application.dto

import com.synchtask.chat.domain.entity.ChatRoom

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
