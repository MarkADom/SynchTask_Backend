package com.synchtask.websocket

import com.synchtask.dtos.chat.WebSocketMessageDTO
import com.synchtask.services.chat.ChatWebSocketService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.LocalDateTime

class ChatWebSocketHandlerTest {

    private lateinit var chatService: ChatWebSocketService
    private lateinit var simpMessagingTemplate: SimpMessagingTemplate
    private lateinit var handler: ChatWebSocketHandler

    @BeforeEach
    fun setup() {
        chatService = mockk(relaxed = true)
        simpMessagingTemplate = mockk(relaxed = true)
        handler = ChatWebSocketHandler(chatService, simpMessagingTemplate)
    }

    @Test
    fun `should process and broadcast valid encrypted message`() {
        val dto = WebSocketMessageDTO(
            chatRoomId = 42L,
            senderEmail = "user@example.com",
            encryptedMessage = "EncryptedPayload",
            timestamp = LocalDateTime.now()
        )

        handler.handleMessage(dto)

        verify {
            chatService.sendMessage(42L, "user@example.com", "EncryptedPayload")
            simpMessagingTemplate.convertAndSend("/topic/chat/42", dto)
        }
    }

    @Test
    fun `should throw exception when encrypted message is blank`() {
        val dto = WebSocketMessageDTO(
            chatRoomId = 42L,
            senderEmail = "user@example.com",
            encryptedMessage = "   ", // blank string
            timestamp = LocalDateTime.now()
        )

        val exception = assertThrows<IllegalArgumentException> {
            handler.handleMessage(dto)
        }

        assert(exception.message!!.contains("Message cannot be empty"))
    }
}
