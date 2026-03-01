package com.synchtask.friend.application.mapper

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional

class FriendMapperTest {
    private val userRepository = mockk<UserRepository>()
    private val mapper = FriendMapper(userRepository)

    @Test
    fun `toResponse uses usersById map when available`() {
        val requester = User(
            id = 1L,
            name = "Requester",
            email = "requester@test.com",
            passwordHash = "hash"
        )
        val friendUser = User(
            id = 2L,
            name = "Friend",
            email = "friend@test.com",
            passwordHash = "hash"
        )
        val friend = Friend(
            id = 10L,
            requesterId = 1L,
            friendId = 2L,
            status = FriendshipStatus.PENDING
        )

        val response =
            mapper.toResponse(
                friend,
                currentUserId = 2L,
                usersById = mapOf(1L to requester, 2L to friendUser)
            )

        assertEquals("requester@test.com", response.friendEmail)
        assertEquals(true, response.isIncoming)
    }

    @Test
    fun `toResponse throws when user cannot be resolved`() {
        val friend = Friend(
            id = 10L,
            requesterId = 1L,
            friendId = 2L,
            status = FriendshipStatus.PENDING
        )
        every { userRepository.findById(1L) } returns Optional.empty()

        assertThrows(ResourceNotFoundException::class.java) {
            mapper.toResponse(friend, currentUserId = 2L, usersById = emptyMap())
        }
    }
}
