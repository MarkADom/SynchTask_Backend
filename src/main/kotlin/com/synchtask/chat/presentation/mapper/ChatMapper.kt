package com.synchtask.chat.presentation.mapper

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.shared.presentation.mapper.MapperSupport.requireId

object ChatMapper {
    fun toRoomDto(chatRoom: ChatRoom): ChatRoomDTO = ChatRoomDTO(
        id = requireId(chatRoom.id, "ChatRoom"),
        participants = chatRoom.participants.map { it.email }
    )

    fun toMessageDto(chatMessage: ChatMessage): ChatMessageDTO = ChatMessageDTO(
        id = requireId(chatMessage.id, "ChatMessage"),
        chatRoomId = requireId(chatMessage.chatRoom.id, "ChatRoom"),
        senderEmail = chatMessage.sender.email,
        message = chatMessage.encryptedMessage,
        timestamp = chatMessage.timestamp
    )
}
