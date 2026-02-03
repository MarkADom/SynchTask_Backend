package com.synchtask.websocket.application.handler

import com.synchtask.chat.application.dto.WebSocketMessageDTO
import com.synchtask.chat.application.service.ChatWebSocketService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    fun `should ignore message when encrypted message is blank`() {
        val dto = WebSocketMessageDTO(
            chatRoomId = 42L,
            senderEmail = "user@example.com",
            encryptedMessage = "   ",
            timestamp = LocalDateTime.now()
        )

        handler.handleMessage(dto)

        // não chama o serviço
        verify(exactly = 0) {
            chatService.sendMessage(any(), any(), any())
        }

        // não envia para websocket
        verify(exactly = 0) {
            simpMessagingTemplate.convertAndSend(any<String>(), any(), any(), any())
        }
    }



}
