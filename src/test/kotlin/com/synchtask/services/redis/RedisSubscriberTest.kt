package com.synchtask.services.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.dtos.notification.NotificationDTO
import com.synchtask.entities.NotificationType
import com.synchtask.managers.WebSocketManager
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.LocalDateTime

class RedisSubscriberTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var logger: Logger
    private lateinit var redisSubscriber: RedisSubscriber

    @BeforeEach
    fun setup() {
        messagingTemplate = mockk(relaxed = true)
        objectMapper = spyk(ObjectMapper().findAndRegisterModules()) // Enables LocalDateTime support
        webSocketManager = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        redisSubscriber = spyk(
            RedisSubscriber(messagingTemplate, objectMapper, webSocketManager),
            recordPrivateCalls = true
        )

        // Override the private logger with a mock
        redisSubscriber.apply {
            val field = this::class.java.getDeclaredField("logger")
            field.isAccessible = true
            field.set(this, logger)
        }
    }

    @Test
    fun `should forward valid chat message to websocket`() {
        val dto = ChatMessageDTO(
            id = 1L,
            chatRoomId = 123L,
            senderEmail = "sender@example.com",
            message = "Hello world!",
            timestamp = LocalDateTime.now()
        )

        val json = objectMapper.writeValueAsString(dto)

        redisSubscriber.handleMessage(json)

        verify {
            messagingTemplate.convertAndSend(
                eq("/topic/chat/${dto.chatRoomId}"),
                match<Any> {
                    it is ChatMessageDTO &&
                            it.id == dto.id &&
                            it.chatRoomId == dto.chatRoomId &&
                            it.senderEmail == dto.senderEmail &&
                            it.message == dto.message
                }
            )
        }
    }

    @Test
    fun `should forward valid notification to websocket`() {
        val dto = NotificationDTO(
            id = 1L,
            recipientEmail = "user@example.com",
            message = "New notification",
            timestamp = LocalDateTime.now(),
            read = false,
            type = NotificationType.TASK_UPDATE
        )

        val json = objectMapper.writeValueAsString(dto)

        redisSubscriber.handleNotificationMessage(json)

        verify {
            messagingTemplate.convertAndSend(
                eq("/user/queue/notifications/${dto.recipientEmail}"),
                match<Any> {
                    it is NotificationDTO &&
                            it.id == dto.id &&
                            it.recipientEmail == dto.recipientEmail &&
                            it.message == dto.message &&
                            it.read == dto.read &&
                            it.type == dto.type
                }
            )
        }
    }

    @Test
    fun `should log error on invalid chat message JSON`() {
        val invalidJson = """{ "id": 1, "chatRoomId": "not_a_number" }"""

        redisSubscriber.handleMessage(invalidJson)

        verify {
            logger.error(
                any<String>(),
                any<String>(),
                any<Throwable>()
            )
        }

    }

    @Test
    fun `should log error on invalid notification JSON`() {
        val invalidJson = """{ "id": 1, "recipientEmail": true }"""

        redisSubscriber.handleNotificationMessage(invalidJson)

        verify {
            logger.error(
                any<String>(),
                any<String>(),
                any<Throwable>()
            )
        }

    }
}
