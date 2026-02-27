package com.synchtask.chat.presentation.contoller

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.application.service.ChatService
import com.synchtask.chat.application.service.KeyExchangeService
import com.synchtask.chat.presentation.controller.ChatController
import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.shared.exception.UnauthorizedAccessException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class ChatControllerTest {
    private lateinit var chatService: ChatService
    private lateinit var keyExchangeService: KeyExchangeService
    private lateinit var controller: ChatController

    @BeforeEach
    fun setUp() {
        chatService = mockk()
        keyExchangeService = mockk()
        controller = ChatController(chatService, keyExchangeService)
    }

    @Test
    fun `should get or create chat room`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        val room = ChatRoomDTO(7L, listOf("me@example.com", "friend@example.com"))
        every { chatService.getOrCreateChatRoom(listOf("me@example.com", "friend@example.com")) } returns room

        val response = controller.getOrCreateChatRoom(principal, "friend@example.com")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(room, response.body)
        verify(exactly = 1) { chatService.getOrCreateChatRoom(listOf("me@example.com", "friend@example.com")) }
    }

    @Test
    fun `should send message`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        val message = ChatMessageDTO(1L, 9L, "me@example.com", "hello", LocalDateTime.now())
        every { chatService.sendMessage(9L, "me@example.com", "hello") } returns message

        val response = controller.sendMessage(9L, principal, "hello")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(message, response.body)
        verify(exactly = 1) { chatService.sendMessage(9L, "me@example.com", "hello") }
    }

    @Test
    fun `should return chat history`() {
        val principal = mockk<UserDetails>()
        val history = listOf(ChatMessageDTO(1L, 4L, "me@example.com", "m1", LocalDateTime.now()))
        every { chatService.getChatHistory(4L) } returns history

        val response = controller.getChatHistory(4L, principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(history, response.body)
        verify(exactly = 1) { chatService.getChatHistory(4L) }
    }

    @Test
    fun `should save public key`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        every { keyExchangeService.saveUserPublicKey("me@example.com", "pubkey") } just runs

        val response = controller.savePublicKey(principal, "pubkey")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(ApiMessageResponseDTO("Public key saved successfully."), response.body)
        verify(exactly = 1) { keyExchangeService.saveUserPublicKey("me@example.com", "pubkey") }
    }

    @Test
    fun `should return my public key when present`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        every { keyExchangeService.getUserPublicKey("me@example.com") } returns "pubkey"

        val response = controller.getMyPublicKey(principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("pubkey", response.body)
    }

    @Test
    fun `should return not found for my public key when missing`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        every { keyExchangeService.getUserPublicKey("me@example.com") } returns null

        val response = controller.getMyPublicKey(principal)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `should return public key by email when present`() {
        val principal = mockk<UserDetails> { every { username } returns "friend@example.com" }

        every { keyExchangeService.getUserPublicKey("friend@example.com") } returns "friend-key"

        val response = controller.getPublicKey("friend@example.com", principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("friend-key", response.body)
    }

    @Test
    fun `should return not found public key by email when missing`() {
        val principal = mockk<UserDetails> { every { username } returns "friend@example.com" }
        every { keyExchangeService.getUserPublicKey("friend@example.com") } returns null

        val response = controller.getPublicKey("friend@example.com", principal)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `should deny legacy public key lookup for different principal`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }

        val ex = assertFailsWith<UnauthorizedAccessException> {
            controller.getPublicKey("friend@example.com", principal)
        }

        assertEquals(
            "Deprecated endpoint only supports the authenticated principal; use /chat/key-exchange/me",
            ex.message
        )
    }

}
