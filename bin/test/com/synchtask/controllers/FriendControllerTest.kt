package com.synchtask.controllers

import com.synchtask.dtos.friend.FriendRequestDTO
import com.synchtask.entities.Friend
import com.synchtask.entities.FriendshipStatus
import com.synchtask.entities.User
import com.synchtask.services.friend.FriendService
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FriendControllerTest {

    private lateinit var friendService: FriendService
    private lateinit var controller: FriendController
    private lateinit var authentication: Authentication

    @BeforeEach
    fun setup() {
        friendService = mockk(relaxed = true)
        controller = FriendController(friendService)

        // Mock SecurityContextHolder
        authentication = mockk()
        every { authentication.name } returns "user@email.com"
        val securityContext = mockk<org.springframework.security.core.context.SecurityContext>()
        every { securityContext.authentication } returns authentication
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `should send friend request`() {
        val request = FriendRequestDTO("friend@email.com")

        every { friendService.sendFriendRequest("user@email.com", "friend@email.com") } returns mockk()

        val response = controller.sendFriendRequest(request)

        assertEquals("Friend request sent successfully!", response.body)
        assertEquals(200, response.statusCode.value())
        verify { friendService.sendFriendRequest("user@email.com", "friend@email.com") }
    }

    @Test
    fun `should accept friend request`() {
        val requestId = 123L

        every { friendService.acceptFriendRequest(requestId) } returns mockk()

        val response = controller.acceptFriendRequest(requestId)

        assertEquals("Friend request accepted!", response.body)
        assertEquals(200, response.statusCode.value())
        verify { friendService.acceptFriendRequest(requestId) }
    }

    @Test
    fun `should remove friend`() {
        val friendId = 456L

        every { friendService.removeFriend(friendId) } just Runs

        val response = controller.removeFriend(friendId)

        assertEquals("Friend removed!", response.body)
        assertEquals(200, response.statusCode.value())
        verify { friendService.removeFriend(friendId) }
    }

    @Test
    fun `should list friends of current user`() {
        val user = User(id = 1L, email = "user@email.com", name = "User", passwordHash = "hash")
        val friend = User(id = 2L, email = "friend@email.com", name = "Friend", passwordHash = "hash2")

        val friendEntity = Friend(
            id = 100L,
            requester = user,
            friend = friend,
            status = FriendshipStatus.ACCEPTED,
            createdAt = LocalDateTime.now()
        )

        every { friendService.listFriends("user@email.com") } returns listOf(friendEntity)

        val response = controller.listFriends()

        assertEquals(200, response.statusCode.value())
        assertEquals(1, response.body?.size)
        val friendDto = response.body?.first()

        assertNotEquals("user@email.com", friendDto?.friendEmail)
        assertEquals("friend@email.com", friendDto?.friendEmail)
        assertEquals("ACCEPTED", friendDto?.status)


        verify { friendService.listFriends("user@email.com") }
    }
}
