package com.synchtask.mappers

import com.synchtask.dtos.chat.ChatMessageDTO
import com.synchtask.dtos.chat.ChatRoomDTO
import com.synchtask.entities.ChatMessage
import com.synchtask.entities.ChatRoom

object ChatMapper {

    fun toChatRoomDTO(chatRoom: ChatRoom): ChatRoomDTO {
        return ChatRoomDTO(
            id = chatRoom.id!!,
            participants = chatRoom.participants.map { it.email }
        )
    }

    fun toChatMessageDTO(chatMessage: ChatMessage): ChatMessageDTO {
        return ChatMessageDTO(
            id = chatMessage.id!!,
            chatRoomId = chatMessage.chatRoom.id!!,
            senderEmail = chatMessage.sender.email,
            message = chatMessage.encryptedMessage,
            timestamp = chatMessage.timestamp
        )
    }
}
