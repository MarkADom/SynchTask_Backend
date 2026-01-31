package com.synchtask.controllers

import com.synchtask.dtos.chat.ChatMessageDTO
import com.synchtask.dtos.chat.ChatRoomDTO
import com.synchtask.services.chat.ChatService
import com.synchtask.services.chat.KeyExchangeService
import io.mockk.every
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

    private val testUser = User("user@example.com", "password", emptyList())

    @BeforeEach
    fun setup() {
        chatService = mockk()
        keyExchangeService = mockk()
        controller = ChatController(chatService, keyExchangeService)
    }

    @Test
    fun `should get or create chat room`() {
        val friendEmail = "friend@example.com"
        val roomDto = ChatRoomDTO(id = 1L, participants = listOf("user@example.com", friendEmail))

        every {
            chatService.getOrCreateChatRoom(listOf("user@example.com", friendEmail))
        } returns roomDto

        val response = controller.getOrCreateChatRoom(testUser, friendEmail)

        assertEquals(ResponseEntity.ok(roomDto), response)
        verify { chatService.getOrCreateChatRoom(listOf("user@example.com", friendEmail)) }
    }

    @Test
    fun `should send message to chat room`() {
        val messageDto = ChatMessageDTO(
            id = 1L,
            chatRoomId = 10L,
            senderEmail = "user@example.com",
            message = "Hello",
            timestamp = LocalDateTime.now()
        )

        every {
            chatService.sendMessage(10L, "user@example.com", "Hello")
        } returns messageDto

        val response = controller.sendMessage(10L, testUser, "Hello")

        assertEquals(ResponseEntity.ok(messageDto), response)
        verify { chatService.sendMessage(10L, "user@example.com", "Hello") }
    }

    @Test
    fun `should return chat history`() {
        val chatRoomId = 5L
        val history = listOf(
            ChatMessageDTO(1L, chatRoomId, "user@example.com", "Hello", LocalDateTime.now()),
            ChatMessageDTO(2L, chatRoomId, "friend@example.com", "Hi", LocalDateTime.now())
        )

        every { chatService.getChatHistory(chatRoomId) } returns history

        val response = controller.getChatHistory(chatRoomId)

        assertEquals(ResponseEntity.ok(history), response)
        verify { chatService.getChatHistory(chatRoomId) }
    }

    @Test
    fun `should save user public key`() {
        val userEmail = "user@example.com"
        val publicKey = "mockPublicKey"

        every { keyExchangeService.saveUserPublicKey(userEmail, publicKey) } returns Unit

        val response = controller.savePublicKey(userEmail, publicKey)

        assertEquals(ResponseEntity.ok("Public key saved successfully."), response)
        verify { keyExchangeService.saveUserPublicKey(userEmail, publicKey) }
    }

    @Test
    fun `should return public key if exists`() {
        val email = "user@example.com"
        val publicKey = "mockPublicKey"

        every { keyExchangeService.getUserPublicKey(email) } returns publicKey

        val response = controller.getPublicKey(email)

        assertEquals(ResponseEntity.ok(publicKey), response)
        verify { keyExchangeService.getUserPublicKey(email) }
    }

    @Test
    fun `should return 404 if public key not found`() {
        val email = "notfound@example.com"

        every { keyExchangeService.getUserPublicKey(email) } returns null

        val response = controller.getPublicKey(email)

        assertEquals(ResponseEntity.notFound().build(), response)
        verify { keyExchangeService.getUserPublicKey(email) }
    }
}
