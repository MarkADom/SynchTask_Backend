package com.synchtask.services.chat

import com.synchtask.dtos.chat.ChatMessageDTO
import com.synchtask.dtos.chat.ChatRoomDTO
import com.synchtask.entities.ChatMessage
import com.synchtask.entities.ChatRoom
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.repositories.ChatMessageRepository
import com.synchtask.repositories.ChatRoomRepository
import com.synchtask.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * **Chat Service**
 *
 * Manages chat messages and rooms. Ensures proper validation, security, and clean architecture.
 */
@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    /**
     * Creates or retrieves a chat room between users.
     *
     * @param participants List of user emails (minimum 2).
     * @return DTO of the existing or newly created chat room.
     * @throws ResourceNotFoundException if any user does not exist.
     */
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

    /**
     * Sends a message in a specific chat room.
     *
     * @param chatRoomId The ID of the chat room.
     * @param senderEmail The email of the sender.
     * @param message The encrypted message.
     * @return DTO of the saved chat message.
     * @throws ResourceNotFoundException if a chat room or sender is not found.
     * @throws UnauthorizedAccessException if the sender is not a participant.
     */
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

    /**
     * Retrieves the chat history for a specific chat room.
     *
     * @param chatRoomId ID of the chat room.
     * @return List of messages in chronological order.
     * @throws ResourceNotFoundException if the chat room does not exist.
     */
    fun getChatHistory(chatRoomId: Long): List<ChatMessageDTO> {
        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found") }

        val messages = chatMessageRepository.findByChatRoomOrderByTimestampAsc(chatRoom)
        logger.info("Retrieved ${messages.size} messages from chatRoom ${chatRoom.id}")

        return messages.map { ChatMessageDTO.fromEntity(it) }
    }
}
