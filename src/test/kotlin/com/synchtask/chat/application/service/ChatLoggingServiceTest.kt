package com.synchtask.chat.application.service

import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.chat.domain.repository.ChatMessageRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class ChatLoggingServiceTest {
    @MockK
    lateinit var chatMessageRepository: ChatMessageRepository

    @InjectMockKs
    lateinit var chatLoggingService: ChatLoggingService

    private lateinit var sender: User
    private lateinit var chatRoom: ChatRoom
    private lateinit var chatMessage: ChatMessage

    @BeforeEach
    fun setUp() {
        sender =
            User(
                id = 1L,
                name = "Alice",
                email = "alice@synchtask.com",
                passwordHash = "hashedPassword",
                role = UserRole.USER
            )

        chatRoom = ChatRoom(id = 100L)

        chatMessage =
            ChatMessage(
                id = 10L,
                chatRoom = chatRoom,
                sender = sender,
                encryptedMessage = "Encrypted hello world",
                timestamp = LocalDateTime.now()
            )
    }

    @Test
    fun `should log and save chat message`() {
        every { chatMessageRepository.save(chatMessage) } returns chatMessage

        chatLoggingService.logMessage(chatMessage)

        verify(exactly = 1) { chatMessageRepository.save(chatMessage) }
    }

    @Test
    fun `should throw exception when repository fails`() {
        every { chatMessageRepository.save(chatMessage) } throws RuntimeException("DB down")

        val ex =
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                chatLoggingService.logMessage(chatMessage)
            }

        assertEquals("Unable to persist chat message.", ex.message)

        verify(exactly = 1) { chatMessageRepository.save(chatMessage) }
    }

    @Test
    fun `should count messages`() {
        every { chatMessageRepository.count() } returns 42L

        val result = chatLoggingService.countMessages()

        assertEquals(42L, result)
        verify(exactly = 1) { chatMessageRepository.count() }
    }

    @Test
    fun `should clear all logs and return deleted count`() {
        every { chatMessageRepository.count() } returns 10L
        every { chatMessageRepository.deleteAll() } returns Unit

        val result = chatLoggingService.clearAllLogs()

        assertEquals(10L, result)
        verifyOrder {
            chatMessageRepository.count()
            chatMessageRepository.deleteAll()
        }
    }
}
