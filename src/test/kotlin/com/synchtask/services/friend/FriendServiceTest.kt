package com.synchtask.services.friend

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.friend.domain.exception.FriendRequestAlreadySentException
import com.synchtask.friend.application.service.FriendService
import com.synchtask.friend.domain.repository.FriendRepository
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.notification.application.service.NotificationService
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*

class FriendServiceTest {

    private lateinit var friendRepository: FriendRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationService: NotificationService
    private lateinit var service: FriendService

    private val alice = User(
        id = 1L,
        name = "Alice",
        email = "alice@example.com",
        passwordHash = "123",
        role = UserRole.USER
    )

    private val bob = User(
        id = 2L,
        name = "Bob",
        email = "bob@example.com",
        passwordHash = "456",
        role = UserRole.USER
    )

    @BeforeEach
    fun setUp() {
        friendRepository = mockk()
        userRepository = mockk()
        notificationService = mockk(relaxed = true)
        service = FriendService(friendRepository, userRepository, notificationService)
    }

    @Test
    fun `should send friend request`() {
        every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
        every { userRepository.findByEmail(bob.email) } returns Optional.of(bob)
        every { friendRepository.findByRequesterAndFriend(any(), any()) } returns null
        every { friendRepository.save(any()) } answers { firstArg() }

        val result = service.sendFriendRequest(alice.email, bob.email)

        assertEquals(alice, result.requester)
        assertEquals(bob, result.friend)
        assertEquals(FriendshipStatus.PENDING, result.status)

        verify { notificationService.sendNotification(any(), any(), any(), any()) }
    }

    @Test
    fun `should throw if friend request already exists`() {
        every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
        every { userRepository.findByEmail(bob.email) } returns Optional.of(bob)
        every {
            friendRepository.findByRequesterAndFriend(alice, bob)
        } returns Friend(requester = alice, friend = bob)

        val ex = assertThrows<FriendRequestAlreadySentException> {
            service.sendFriendRequest(alice.email, bob.email)
        }

        assertEquals("Friend request already exists!", ex.message)
    }

    @Test
    fun `should accept friend request`() {
        val request = Friend(
            id = 1L,
            requester = alice,
            friend = bob,
            status = FriendshipStatus.PENDING
        )

        every { friendRepository.findById(1L) } returns Optional.of(request)
        every { friendRepository.save(any()) } answers { firstArg() }

        val result = service.acceptFriendRequest(1L, bob.email)

        assertEquals(FriendshipStatus.ACCEPTED, result.status)
        verify { notificationService.sendNotification(any(), any(), any(), any()) }
    }

    @Test
    fun `should remove friend`() {
        val request = Friend(id = 1L, requester = alice, friend = bob)

        every { friendRepository.findById(1L) } returns Optional.of(request)
        every { friendRepository.delete(request) } just Runs

        service.removeFriend(1L, alice.email)

        verify { friendRepository.delete(request) }
    }

    @Test
    fun `should list all friend relations`() {
        every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
        every { friendRepository.findAllByUserInvolved(alice) } returns listOf(
            Friend(requester = alice, friend = bob, status = FriendshipStatus.ACCEPTED)
        )

        val result = service.listFriends(alice.email)

        assertEquals(1, result.size)
        assertEquals(bob, result.first().friend)
    }

    @Test
    fun `should list accepted friend users`() {
        every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
        every {
            friendRepository.findByRequesterAndStatus(alice, FriendshipStatus.ACCEPTED)
        } returns listOf(Friend(requester = alice, friend = bob, status = FriendshipStatus.ACCEPTED))

        every {
            friendRepository.findByFriendAndStatus(alice, FriendshipStatus.ACCEPTED)
        } returns emptyList()

        val users = service.listFriendUsers(alice.email)

        assertEquals(1, users.size)
        assertEquals(bob, users.first())
    }
}
