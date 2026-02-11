package com.synchtask.friend.application.service

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.friend.domain.exception.FriendRequestAlreadySentException
import com.synchtask.friend.domain.repository.FriendRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import io.mockk.*

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
        friendRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        service = FriendService(friendRepository, userRepository, notificationService)
    }

    @Test
    fun `should send friend request`() {
        every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
        every { userRepository.findByEmail(bob.email) } returns Optional.of(bob)
        every { friendRepository.findByRequesterIdAndFriendId(any(), any()) } returns null
        every { friendRepository.save(any()) } answers { firstArg() }

        val result = service.sendFriendRequest(alice.email, bob.email)

        Assertions.assertEquals(alice, result.requesterId)
        Assertions.assertEquals(bob, result.friendId)
        Assertions.assertEquals(FriendshipStatus.PENDING, result.status)

        verify { notificationService.sendNotification(any(), any(), any(), any()) }
    }

    @Test
    fun `should throw if friend request already exists in direct direction`() {
        every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
        every { userRepository.findByEmail(bob.email) } returns Optional.of(bob)
        every {
            friendRepository.findByRequesterIdAndFriendId(alice.id!!, bob.id!!)
        } returns Friend(requesterId = alice.id!!, friendId = bob.id!!)

        assertThrows<FriendRequestAlreadySentException> {
            service.sendFriendRequest(alice.email, bob.email)
        }

        assertThrows<FriendRequestAlreadySentException> {
            service.sendFriendRequest(alice.email, bob.email)
        }


        @Test
        fun `should throw if friend request already exists in reverse direction`() {
            every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
            every { userRepository.findByEmail(bob.email) } returns Optional.of(bob)
            every { friendRepository.findByRequesterIdAndFriendId(alice.id!!, bob.id!!) } returns null
            every {
                friendRepository.findByRequesterIdAndFriendId(
                    bob.id!!,
                    alice.id!!
                )
            } returns Friend(requesterId = bob.id!!, friendId = alice.id!!)

            assertThrows<FriendRequestAlreadySentException> {
                service.sendFriendRequest(alice.email, bob.email)
            }
        }

        @Test
        fun `should accept friend request`() {
            val request = Friend(
                id = 1L,
                requesterId = alice.id!!,
                friendId = bob.id!!,
                status = FriendshipStatus.PENDING
            )


            every { userRepository.findByEmail(bob.email) } returns Optional.of(bob)
            every { friendRepository.findById(1L) } returns Optional.of(request)
            every { friendRepository.save(any()) } answers { firstArg() }

            val result = service.acceptFriendRequest(1L, bob.email)

            Assertions.assertEquals(FriendshipStatus.ACCEPTED, result.status)
            verify { notificationService.sendNotification(any(), any(), any(), any()) }
        }


        @Test
        fun `should reject accept by non target user`() {
            val request =
                Friend(id = 1L, requesterId = alice.id!!, friendId = bob.id!!, status = FriendshipStatus.PENDING)
            every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
            every { friendRepository.findById(1L) } returns Optional.of(request)

            assertThrows<UnauthorizedAccessException> {
                service.acceptFriendRequest(1L, alice.email)
            }
        }

        @Test
        fun `should remove friend`() {
            val request = Friend(id = 1L, requesterId = alice.id!!, friendId = bob.id!!)

            every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
            every { friendRepository.findById(1L) } returns Optional.of(request)
            every { friendRepository.delete(request) } just Runs

            service.removeFriend(1L, alice.email)

            verify { friendRepository.delete(request) }
        }

        @Test
        fun `should list all friend relations`() {
            every { userRepository.findByEmail(alice.email) } returns Optional.of(alice)
            every { friendRepository.findAllByRequesterIdOrFriendId(alice.id!!, alice.id!!) } returns listOf(
                Friend(requesterId = alice.id!!, friendId = bob.id!!, status = FriendshipStatus.ACCEPTED)
            )

            val result = service.listFriends(alice.email)

            Assertions.assertEquals(1, result.size)
            Assertions.assertEquals("bob@example.com", result.first().friendEmail)
        }

    }
}
