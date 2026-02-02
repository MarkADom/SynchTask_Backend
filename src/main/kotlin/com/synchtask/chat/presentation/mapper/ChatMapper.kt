package com.synchtask.chat.presentation.mapper

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom

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
