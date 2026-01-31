package com.synchtask.services.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.synchtask.context.ChatServiceContext
import com.synchtask.dtos.chat.WebSocketMessageDTO
import com.synchtask.entities.ChatMessage
import com.synchtask.entities.ChatRoom
import com.synchtask.entities.User
import com.synchtask.managers.WebSocketManager
import com.synchtask.repositories.ChatMessageRepository
import com.synchtask.repositories.ChatRoomRepository
import com.synchtask.repositories.UserRepository
import com.synchtask.services.redis.RedisPublisher
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.LocalDateTime
import java.util.*

class ChatWebSocketServiceTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var chatRoomRepository: ChatRoomRepository
    private lateinit var userRepository: UserRepository
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var redisPublisher: RedisPublisher
    private lateinit var objectMapper: ObjectMapper
    private lateinit var chatServiceContext: ChatServiceContext
    private lateinit var chatWebSocketService: ChatWebSocketService

    @BeforeEach
    fun setup() {
        messagingTemplate = mockk(relaxed = true)
        chatMessageRepository = mockk()
        chatRoomRepository = mockk()
        userRepository = mockk()
        webSocketManager = mockk()
        redisPublisher = mockk(relaxed = true)
        objectMapper = mockk()

        chatServiceContext = ChatServiceContext(
            messagingTemplate,
            chatMessageRepository,
            chatRoomRepository,
            userRepository,
            webSocketManager,
            redisPublisher,
            objectMapper
        )

        chatWebSocketService = ChatWebSocketService(chatServiceContext)
    }

    @Test
    fun `should send encrypted WebSocket message and store it`() {
        // Arrange
        val senderEmail = "alice@example.com"
        val message = "encrypted-content"
        val chatRoomId = 99L
        val sender = User(name = "Alice", email = senderEmail, passwordHash = "123")
        val chatRoom = ChatRoom(id = chatRoomId, participants = mutableSetOf(sender))
        val savedMessage = ChatMessage(
            id = 1L,
            chatRoom = chatRoom,
            sender = sender,
            encryptedMessage = message,
            timestamp = LocalDateTime.now()
        )

        every { chatRoomRepository.findById(chatRoomId) } returns Optional.of(chatRoom)
        every { userRepository.findByEmail(senderEmail) } returns Optional.of(sender)
        every { chatMessageRepository.save(any()) } returns savedMessage

        // Act
        chatWebSocketService.sendMessage(chatRoomId, senderEmail, message)

        // Assert
        verify {
            chatMessageRepository.save(match {
                it.sender == sender && it.chatRoom == chatRoom && it.encryptedMessage == message
            })
            messagingTemplate.convertAndSend("/topic/chat/$chatRoomId", any<WebSocketMessageDTO>())
            redisPublisher.publish("chat-messages", message)
        }
    }

    @Test
    fun `should notify online user via WebSocket`() {
        // Arrange
        val userEmail = "bob@example.com"
        val message = "New notification"
        every { webSocketManager.isUserOnline(userEmail) } returns true

        // Act
        chatWebSocketService.notifyUser(userEmail, message)

        // Assert
        verify {
            messagingTemplate.convertAndSend("/queue/notifications/$userEmail", message)
        }
    }

    @Test
    fun `should not notify offline user via WebSocket`() {
        // Arrange
        val userEmail = "bob@example.com"
        val message = "Should not be sent"
        every { webSocketManager.isUserOnline(userEmail) } returns false

        // Act
        chatWebSocketService.notifyUser(userEmail, message)

        // Assert
        verify(exactly = 0) {
            messagingTemplate.convertAndSend(any(), any<String>())
        }
    }
}
