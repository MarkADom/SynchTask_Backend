package com.synchtask.services.chat

import com.synchtask.chat.application.service.ChatLoggingService
import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.user.domain.entity.User
import com.synchtask.chat.domain.repository.ChatMessageRepository
import com.synchtask.user.domain.entity.UserRole
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

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
        sender = User(
            id = 1L,
            name = "Alice",
            email = "alice@synchtask.com",
            passwordHash = "hashedPassword",
            role = UserRole.USER
        )

        chatRoom = ChatRoom(id = 100L)

        chatMessage = ChatMessage(
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
}
