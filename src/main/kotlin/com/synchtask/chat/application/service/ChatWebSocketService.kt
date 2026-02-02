package com.synchtask.chat.application.service

import com.synchtask.context.ChatServiceContext
import com.synchtask.chat.application.dto.WebSocketMessageDTO
import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ChatWebSocketService(
    private val chatContext: ChatServiceContext,
) {

    private val logger = LoggerFactory.getLogger(ChatWebSocketService::class.java)

    @Transactional
    fun sendMessage(chatRoomId: Long, senderEmail: String, encryptedMessage: String) {
        require(encryptedMessage.isNotBlank()) {
            "Message cannot be empty. Only encrypted messages are allowed."
        }

        val chatRoom = chatContext.chatRoomRepository.findById(chatRoomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found") }

        val sender = chatContext.userRepository.findByEmail(senderEmail)
            .orElseThrow { ResourceNotFoundException("Sender not found") }

        if (chatRoom.participants.none { it.email == senderEmail }) {
            throw UnauthorizedAccessException("Sender is not a participant of the chat room")
        }

        val chatMessage = chatContext.chatMessageRepository.save(
            ChatMessage(
                chatRoom = chatRoom,
                sender = sender,
                encryptedMessage = encryptedMessage,
                timestamp = LocalDateTime.now()
            )
        )

        val webSocketMessage = WebSocketMessageDTO(
            chatRoomId = chatRoom.id!!,
            senderEmail = sender.email,
            encryptedMessage = encryptedMessage,
            timestamp = chatMessage.timestamp
        )

        chatRoom.participants.forEach { participant ->
            chatContext.messagingTemplate.convertAndSend("/topic/chat/${chatRoom.id}", webSocketMessage)
        }

        chatContext.redisPublisher.publish("chat-messages", encryptedMessage)

        logger.info("Encrypted message sent and synchronized in chatRoom ${chatRoom.id}.")
    }

    fun notifyUser(userEmail: String, message: String) {
        if (chatContext.webSocketManager.isUserOnline(userEmail)) {
            val destination = "/queue/notifications/$userEmail"
            chatContext.messagingTemplate.convertAndSend(destination, message)
            logger.info("WebSocket notification sent to $userEmail")
        } else {
            logger.warn("User $userEmail is offline. Notification was not sent via WebSocket.")
        }
    }
}
