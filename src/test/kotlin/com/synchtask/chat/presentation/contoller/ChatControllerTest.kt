package com.synchtask.chat.presentation.contoller

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.application.service.ChatService
import com.synchtask.chat.application.service.KeyExchangeService
import com.synchtask.chat.presentation.controller.ChatController
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.User
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ChatControllerTest {
    private lateinit var chatService: ChatService
    private lateinit var keyExchangeService: KeyExchangeService
    private lateinit var controller: ChatController

    private val testUser =
        User(
            "user@example.com",
            "password",
            emptyList()
        )

    @BeforeEach
    fun setup() {
        chatService = mockk()
        keyExchangeService = mockk()
        controller = ChatController(chatService, keyExchangeService)
    }

    @Test
    fun `should get or create chat room`() {
        val friendEmail = "friend@example.com"
        val roomDto =
            ChatRoomDTO(
                id = 1L,
                participants = listOf("user@example.com", friendEmail)
            )

        every {
            chatService.getOrCreateChatRoom(listOf("user@example.com", friendEmail))
        } returns roomDto

        val response = controller.getOrCreateChatRoom(testUser, friendEmail)

        assertEquals(ResponseEntity.ok(roomDto), response)
        verify {
            chatService.getOrCreateChatRoom(listOf("user@example.com", friendEmail))
        }
    }

    @Test
    fun `should send message to chat room`() {
        val chatRoomId = 10L
        val message = "Hello"

        val messageDto =
            ChatMessageDTO(
                id = 1L,
                chatRoomId = chatRoomId,
                senderEmail = "user@example.com",
                message = message,
                timestamp = LocalDateTime.now()
            )

        every {
            chatService.sendMessage(chatRoomId, "user@example.com", message)
        } returns messageDto

        val response = controller.sendMessage(chatRoomId, testUser, message)

        assertEquals(ResponseEntity.ok(messageDto), response)
        verify {
            chatService.sendMessage(chatRoomId, "user@example.com", message)
        }
    }

    @Test
    fun `should return chat history`() {
        val chatRoomId = 5L
        val history =
            listOf(
                ChatMessageDTO(1L, chatRoomId, "user@example.com", "Hello", LocalDateTime.now()),
                ChatMessageDTO(2L, chatRoomId, "friend@example.com", "Hi", LocalDateTime.now())
            )

        every {
            chatService.getChatHistory(chatRoomId)
        } returns history

        val response = controller.getChatHistory(chatRoomId, testUser)

        assertEquals(ResponseEntity.ok(history), response)
        verify {
            chatService.getChatHistory(chatRoomId)
        }
    }

    @Test
    fun `should save user public key`() {
        val publicKey = "mockPublicKey"

        every {
            keyExchangeService.saveUserPublicKey("user@example.com", publicKey)
        } just Runs

        val response = controller.savePublicKey(testUser, publicKey)

        assertEquals(
            ResponseEntity.ok("Public key saved successfully."),
            response
        )

        verify {
            keyExchangeService.saveUserPublicKey("user@example.com", publicKey)
        }
    }

    @Test
    fun `should return public key if exists`() {
        val email = "user@example.com"
        val publicKey = "mockPublicKey"

        every {
            keyExchangeService.getUserPublicKey(email)
        } returns publicKey

        val response = controller.getPublicKey(email)

        assertEquals(ResponseEntity.ok(publicKey), response)
        verify {
            keyExchangeService.getUserPublicKey(email)
        }
    }

    @Test
    fun `should return 404 if public key not found`() {
        val email = "notfound@example.com"

        every {
            keyExchangeService.getUserPublicKey(email)
        } returns null

        val response = controller.getPublicKey(email)

        assertEquals(ResponseEntity.notFound().build(), response)
        verify {
            keyExchangeService.getUserPublicKey(email)
        }
    }
}
