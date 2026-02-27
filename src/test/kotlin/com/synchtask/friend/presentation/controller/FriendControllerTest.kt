package com.synchtask.friend.presentation.controller

import com.synchtask.friend.application.dto.FriendRequestDTO
import com.synchtask.friend.application.dto.FriendResponseDTO
import com.synchtask.friend.application.service.FriendService
import com.synchtask.shared.dto.ApiMessageResponseDTO
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.security.core.userdetails.User

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FriendControllerTest {
    private lateinit var friendService: FriendService
    private lateinit var controller: FriendController

    private val testUser =
        User(
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

        val response = controller.sendFriendRequest(request, testUser)

        Assertions.assertEquals(ApiMessageResponseDTO("Friend request sent successfully!"), response.body)
        Assertions.assertEquals(200, response.statusCode.value())
        verify { friendService.sendFriendRequest("user@email.com", "friend@email.com") }
    }

    @Test
    fun `should accept friend request`() {
        val requestId = 123L

        every { friendService.acceptFriendRequest(requestId, "user@email.com") } returns mockk()

        val response = controller.acceptFriendRequest(requestId, testUser)

        Assertions.assertEquals(ApiMessageResponseDTO("Friend request accepted!"), response.body)
        Assertions.assertEquals(200, response.statusCode.value())
        verify { friendService.acceptFriendRequest(requestId, "user@email.com") }
    }

    @Test
    fun `should remove friend`() {
        val friendId = 456L

        every { friendService.removeFriend(friendId, "user@email.com") } just Runs

        val response = controller.removeFriend(friendId, testUser)

        Assertions.assertEquals(ApiMessageResponseDTO("Friend removed!"), response.body)
        Assertions.assertEquals(200, response.statusCode.value())
        verify { friendService.removeFriend(friendId, "user@email.com") }
    }

    @Test
    fun `should list friends of current user`() {
        every { friendService.listFriends("user@email.com") } returns
            listOf(
                FriendResponseDTO(
                    id = 100L,
                    friendEmail = "friend@email.com",
                    requesterEmail = "user@email.com",
                    status = "ACCEPTED",
                    isIncoming = false
                )
            )

        val response = controller.listFriends(testUser)

        Assertions.assertEquals(200, response.statusCode.value())
        Assertions.assertEquals(1, response.body?.size)
        val friendDto = response.body?.first()

        Assertions.assertEquals("friend@email.com", friendDto?.friendEmail)
        Assertions.assertEquals("ACCEPTED", friendDto?.status)

        verify { friendService.listFriends("user@email.com") }
    }
}
