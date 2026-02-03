package com.synchtask.mappers

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.chat.presentation.mapper.ChatMapper
import com.synchtask.user.domain.entity.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ChatMapperTest {

    @Test
    fun `should map ChatRoom to ChatRoomDTO correctly`() {
        val user1 = User(
            id = 1L,
            name = "Alice",
            email = "alice@example.com",
            passwordHash = "hashed1"
        )
        val user2 = User(
            id = 2L,
            name = "Bob",
            email = "bob@example.com",
            passwordHash = "hashed2"
        )
        val chatRoom = ChatRoom(
            id = 100L,
            participants = mutableSetOf(user1, user2)
        )

        val dto: ChatRoomDTO = ChatMapper.toChatRoomDTO(chatRoom)

        assertEquals(100L, dto.id)
        assertTrue(dto.participants.containsAll(listOf("alice@example.com", "bob@example.com")))
    }

    @Test
    fun `should map ChatMessage to ChatMessageDTO correctly`() {
        val user = User(
            id = 1L,
            name = "Charlie",
            email = "charlie@example.com",
            passwordHash = "hashed3"
        )
        val chatRoom = ChatRoom(
            id = 200L,
            participants = mutableSetOf(user)
        )
        val chatMessage = ChatMessage(
            id = 300L,
            chatRoom = chatRoom,
            sender = user,
            encryptedMessage = "Encrypted Hello",
            timestamp = LocalDateTime.now()
        )

        val dto: ChatMessageDTO = ChatMapper.toChatMessageDTO(chatMessage)

        assertEquals(300L, dto.id)
        assertEquals(200L, dto.chatRoomId)
        assertEquals("charlie@example.com", dto.senderEmail)
        assertEquals("Encrypted Hello", dto.message)
        assertNotNull(dto.timestamp)
    }

    @Test
    fun `should throw when ChatRoom id is null`() {
        val user = User(
            id = 1L,
            name = "Test",
            email = "test@example.com",
            passwordHash = "hash"
        )
        val chatRoom = ChatRoom(id = null, participants = mutableSetOf(user))

        assertThrows(NullPointerException::class.java) {
            ChatMapper.toChatRoomDTO(chatRoom)
        }
    }

    @Test
    fun `should throw when ChatMessage id or chatRoom id is null`() {
        val user = User(
            id = 1L,
            name = "Test",
            email = "test@example.com",
            passwordHash = "hash"
        )
        val chatRoom = ChatRoom(id = null, participants = mutableSetOf(user))
        val message = ChatMessage(
            id = null,
            chatRoom = chatRoom,
            sender = user,
            encryptedMessage = "some msg",
            timestamp = LocalDateTime.now()
        )

        assertThrows(NullPointerException::class.java) {
            ChatMapper.toChatMessageDTO(message)
        }
    }
}
