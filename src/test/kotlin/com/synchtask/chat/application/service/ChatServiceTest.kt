package com.synchtask.chat.application.service

import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.chat.domain.repository.ChatMessageRepository
import com.synchtask.chat.domain.repository.ChatRoomRepository
import com.synchtask.chat.presentation.mapper.ChatMapper
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class ChatServiceTest {
    private lateinit var chatRoomRepository: ChatRoomRepository
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var userRepository: UserRepository
    private lateinit var chatService: ChatService

    private val sender =
        User(
            id = 1L,
            name = "Marco",
            email = "marco@example.com",
            passwordHash = "hashed",
            profilePictureUrl = "",
            role = UserRole.USER,
            isActive = true
        )

    private val recipient =
        User(
            id = 2L,
            name = "Ana",
            email = "ana@example.com",
            passwordHash = "hashed2",
            profilePictureUrl = "",
            role = UserRole.USER,
            isActive = true
        )

    private lateinit var chatRoom: ChatRoom

    @BeforeEach
    fun setup() {
        chatRoomRepository = mockk()
        chatMessageRepository = mockk()
        userRepository = mockk()
        chatService = ChatService(chatRoomRepository, chatMessageRepository, userRepository)

        chatRoom =
            ChatRoom(
                id = 100L,
                participants = mutableSetOf(sender, recipient)
            )
    }

    @Test
    fun `should create a new chat room when no existing room found`() {
        val participants = listOf(sender.email, recipient.email)

        every { userRepository.findByEmailIn(participants) } returns listOf(sender, recipient)
        every { chatRoomRepository.findByExactParticipants(any(), any()) } returns null
        every { chatRoomRepository.save(any()) } returns chatRoom

        val result = chatService.getOrCreateChatRoom(participants)

        assertEquals(ChatMapper.toRoomDto(chatRoom), result)
        verify { chatRoomRepository.save(any()) }
    }

    @Test
    fun `should return existing chat room if found`() {
        val participants = listOf(sender.email, recipient.email)

        every { userRepository.findByEmailIn(participants) } returns listOf(sender, recipient)
        every { chatRoomRepository.findByExactParticipants(any(), any()) } returns chatRoom

        val result = chatService.getOrCreateChatRoom(participants)

        assertEquals(ChatMapper.toRoomDto(chatRoom), result)
        verify(exactly = 0) { chatRoomRepository.save(any()) }
    }

    @Test
    fun `should send a message and persist it`() {
        val messageContent = "Encrypted hello"
        val chatMessage =
            ChatMessage(
                id = 1L,
                chatRoom = chatRoom,
                sender = sender,
                encryptedMessage = messageContent,
                timestamp = LocalDateTime.now()
            )

        every { chatRoomRepository.findById(chatRoom.id!!) } returns Optional.of(chatRoom)
        every { userRepository.findByEmail(sender.email) } returns Optional.of(sender)
        every { chatMessageRepository.save(any()) } returns chatMessage

        val result = chatService.sendMessage(chatRoom.id!!, sender.email, messageContent)

        assertEquals(ChatMapper.toMessageDto(chatMessage), result)
        verify { chatMessageRepository.save(any()) }
    }

    @Test
    fun `should return chat history for given chat room`() {
        val messages =
            listOf(
                ChatMessage(1L, chatRoom, sender, "msg1", LocalDateTime.now().minusMinutes(2)),
                ChatMessage(2L, chatRoom, recipient, "msg2", LocalDateTime.now().minusMinutes(1))
            )

        every { chatRoomRepository.findById(chatRoom.id!!) } returns Optional.of(chatRoom)
        every { chatMessageRepository.findByChatRoomOrderByTimestampAsc(chatRoom) } returns messages

        val result = chatService.getChatHistory(chatRoom.id!!)

        assertEquals(messages.map { ChatMapper.toMessageDto(it) }, result)
    }

    @Test
    fun `should throw if participants list is invalid`() {
        val ex =
            assertThrows<IllegalArgumentException> {
                chatService.getOrCreateChatRoom(listOf("onlyone@example.com"))
            }
        assertEquals("A chat room must have at least 2 participants.", ex.message)
    }

    @Test
    fun `should throw if user is not found for chat room`() {
        val emails = listOf("notfound@example.com", "valid@example.com")
        every { userRepository.findByEmailIn(emails) } returns listOf(sender) // only one resolved

        val ex =
            assertThrows<ResourceNotFoundException> {
                chatService.getOrCreateChatRoom(emails)
            }

        assertEquals("One or more participants not found.", ex.message)
    }

    @Test
    fun `should throw if chat room does not exist on sendMessage`() {
        every { chatRoomRepository.findById(999L) } returns Optional.empty()

        val ex =
            assertThrows<ResourceNotFoundException> {
                chatService.sendMessage(999L, sender.email, "message")
            }

        assertEquals("Chat room not found", ex.message)
    }

    @Test
    fun `should throw if sender not found on sendMessage`() {
        every { chatRoomRepository.findById(chatRoom.id!!) } returns Optional.of(chatRoom)
        every { userRepository.findByEmail(sender.email) } returns Optional.empty()

        val ex =
            assertThrows<ResourceNotFoundException> {
                chatService.sendMessage(chatRoom.id!!, sender.email, "msg")
            }

        assertEquals("Sender not found", ex.message)
    }
}
