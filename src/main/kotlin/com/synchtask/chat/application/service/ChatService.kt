package com.synchtask.chat.application.service

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.chat.domain.repository.ChatMessageRepository
import com.synchtask.chat.domain.repository.ChatRoomRepository
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    @Transactional
    fun getOrCreateChatRoom(participants: List<String>): ChatRoomDTO {
        require(participants.size >= 2) { "A chat room must have at least 2 participants." }

        val users = userRepository.findByEmailIn(participants).toSet()
        if (users.size != participants.size) {
            throw ResourceNotFoundException("One or more participants not found.")
        }

        val existingChat = chatRoomRepository.findByExactParticipants(users, users.size)
        if (existingChat != null) {
            return ChatRoomDTO.fromEntity(existingChat)
        }

        val chatRoom = chatRoomRepository.save(ChatRoom(participants = users.toMutableSet()))
        return ChatRoomDTO.fromEntity(chatRoom)
    }

    @Transactional
    fun sendMessage(chatRoomId: Long, senderEmail: String, message: String): ChatMessageDTO {
        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found") }

        if (chatRoom.participants.none { it.email == senderEmail }) {
            throw UnauthorizedAccessException("Sender is not a participant of the chat room")
        }

        val sender = userRepository.findByEmail(senderEmail)
            .orElseThrow { ResourceNotFoundException("Sender not found") }

        val chatMessage = chatMessageRepository.save(
            ChatMessage(
                chatRoom = chatRoom,
                sender = sender,
                encryptedMessage = message,
                timestamp = LocalDateTime.now()
            )
        )
        return ChatMessageDTO.fromEntity(chatMessage)
    }

    fun getChatHistory(chatRoomId: Long): List<ChatMessageDTO> {
        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found") }

        val messages = chatMessageRepository.findByChatRoomOrderByTimestampAsc(chatRoom)
        logger.info("Retrieved ${messages.size} messages from chatRoom ${chatRoom.id}")

        return messages.map { ChatMessageDTO.fromEntity(it) }
    }
}
