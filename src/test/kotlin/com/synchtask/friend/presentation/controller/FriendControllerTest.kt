package com.synchtask.friend.presentation.controller

import com.synchtask.friend.application.dto.FriendRequestDTO
import com.synchtask.friend.application.service.FriendService
import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FriendControllerTest {

    private lateinit var friendService: FriendService
    private lateinit var controller: FriendController
    private lateinit var authentication: Authentication

    private val testUSer = User(
        "user@email.com",
        "password",
        emptyList()
    )

    @BeforeEach
    fun setup() {
        friendService = mockk(relaxed = true)
        controller = FriendController(friendService)

    }

    @Test
    fun `should send friend request`() {
        val request = FriendRequestDTO("friend@email.com")

        every { friendService.sendFriendRequest("user@email.com", "friend@email.com") } returns mockk()

        val response = controller.sendFriendRequest(request, testUSer)

        Assertions.assertEquals("Friend request sent successfully!", response.body)
        Assertions.assertEquals(200, response.statusCode.value())
        verify { friendService.sendFriendRequest("user@email.com", "friend@email.com") }
    }

    @Test
    fun `should accept friend request`() {
        val requestId = 123L

        every { friendService.acceptFriendRequest(requestId, "user@email.com") } returns mockk()

        val response = controller.acceptFriendRequest(requestId, testUSer)

        Assertions.assertEquals("Friend request accepted!", response.body)
        Assertions.assertEquals(200, response.statusCode.value())
        verify { friendService.acceptFriendRequest(requestId, "user@email.com") }
    }

    @Test
    fun `should remove friend`() {
        val friendId = 456L

        every { friendService.removeFriend(friendId, "user@email.com") } just Runs

        val response = controller.removeFriend(friendId, testUSer)

        Assertions.assertEquals("Friend removed!", response.body)
        Assertions.assertEquals(200, response.statusCode.value())
        verify { friendService.removeFriend(friendId, "user@email.com") }
    }

    @Test
    fun `should list friends of current user`() {
        val user = com.synchtask.user.domain.entity.User(
            id = 1L,
            email = "user@email.com",
            name = "User",
            passwordHash = "hash"
        )
        val friend = com.synchtask.user.domain.entity.User(
            id = 2L,
            email = "friend@email.com",
            name = "Friend",
            passwordHash = "hash2"
        )

        val friendEntity = Friend(
            id = 100L,
            requester = user,
            friend = friend,
            status = FriendshipStatus.ACCEPTED,
            createdAt = LocalDateTime.now()
        )

        every { friendService.listFriends("user@email.com") } returns listOf(friendEntity)

        val response = controller.listFriends(testUSer)

        Assertions.assertEquals(200, response.statusCode.value())
        Assertions.assertEquals(1, response.body?.size)
        val friendDto = response.body?.first()

        Assertions.assertNotEquals("user@email.com", friendDto?.friendEmail)
        Assertions.assertEquals("friend@email.com", friendDto?.friendEmail)
        Assertions.assertEquals("ACCEPTED", friendDto?.status)


        verify { friendService.listFriends("user@email.com") }
    }
}
