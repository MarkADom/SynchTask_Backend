package com.synchtask.services.friend

import com.synchtask.entities.Friend
import com.synchtask.entities.FriendshipStatus
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.FriendRepository
import com.synchtask.repositories.UserRepository
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*

class FriendServiceTest {

    private lateinit var friendRepository: FriendRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: FriendService

    private val alice = User(
        id = 1L,
        name = "Alice",
        email = "alice@example.com",
        passwordHash = "123",
        profilePictureUrl = "",
        role = UserRole.USER
    )

    private val bob = User(
        id = 2L,
        name = "Bob",
        email = "bob@example.com",
        passwordHash = "456",
        profilePictureUrl = "",
        role = UserRole.USER
    )

    @BeforeEach
    fun setUp() {
        friendRepository = mockk()
        userRepository = mockk()
        service = FriendService(friendRepository, userRepository)
    }

    @Test
    fun `should send friend request`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(alice)
        every { userRepository.findByEmail("bob@example.com") } returns Optional.of(bob)
        every { friendRepository.findByRequesterAndFriend(alice, bob) } returns null
        every { friendRepository.save(any()) } answers { firstArg() }

        val result = service.sendFriendRequest("alice@example.com", "bob@example.com")

        assertEquals(alice, result.requester)
        assertEquals(bob, result.friend)
        assertEquals(FriendshipStatus.PENDING, result.status)

        verify(exactly = 1) { friendRepository.save(any()) }
    }


    @Test
    fun `should throw if requester not found`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.empty()

        val ex = assertThrows<ResourceNotFoundException> {
            service.sendFriendRequest("alice@example.com", "bob@example.com")
        }

        assertEquals("User not found: alice@example.com", ex.message)
    }

    @Test
    fun `should throw if friend not found`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(alice)
        every { userRepository.findByEmail("bob@example.com") } returns Optional.empty()

        val ex = assertThrows<ResourceNotFoundException> {
            service.sendFriendRequest("alice@example.com", "bob@example.com")
        }

        assertEquals("User not found: bob@example.com", ex.message)
    }

    @Test
    fun `should throw if friend request already exists`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(alice)
        every { userRepository.findByEmail("bob@example.com") } returns Optional.of(bob)
        every { friendRepository.findByRequesterAndFriend(alice, bob) } returns Friend(requester = alice, friend = bob)

        val ex = assertThrows<IllegalArgumentException> {
            service.sendFriendRequest("alice@example.com", "bob@example.com")
        }

        assertEquals("Friend request already sent!", ex.message)
    }


    @Test
    fun `should accept friend request`() {
        val request = Friend(id = 1L, requester = alice, friend = bob)

        every { friendRepository.findById(1L) } returns Optional.of(request)
        every { friendRepository.save(request) } returns request

        val result = service.acceptFriendRequest(1L)

        assertEquals(FriendshipStatus.ACCEPTED, result.status)
        verify { friendRepository.save(request) }
    }

    @Test
    fun `should throw if friend request to accept not found`() {
        every { friendRepository.findById(1L) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.acceptFriendRequest(1L)
        }
    }

    @Test
    fun `should remove friend`() {
        val request = Friend(id = 1L, requester = alice, friend = bob)

        every { friendRepository.findById(1L) } returns Optional.of(request)
        every { friendRepository.delete(request) } just Runs

        service.removeFriend(1L)

        verify { friendRepository.delete(request) }
    }

    @Test
    fun `should throw if friend request to remove not found`() {
        every { friendRepository.findById(99L) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.removeFriend(99L)
        }
    }

    @Test
    fun `should list all friends`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(alice)
        every {
            friendRepository.findByRequesterAndStatus(alice, FriendshipStatus.ACCEPTED)
        } returns listOf(
            Friend(requester = alice, friend = bob, status = FriendshipStatus.ACCEPTED)
        )
        every {
            friendRepository.findByFriendAndStatus(alice, FriendshipStatus.ACCEPTED)
        } returns emptyList()

        val result = service.listFriends("alice@example.com")

        assertEquals(1, result.size)
        assertEquals(bob, result.first().friend)
    }

    @Test
    fun `should throw if user not found when listing friends`() {
        every { userRepository.findByEmail("unknown@example.com") } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.listFriends("unknown@example.com")
        }
    }

    @Test
    fun `should get user friends' emails`() {
        val friendship1 = Friend(requester = alice, friend = bob, status = FriendshipStatus.ACCEPTED)
        val friendship2 = Friend(requester = bob, friend = alice, status = FriendshipStatus.ACCEPTED)

        every {
            friendRepository.findFriendsByRequesterEmailOrFriendEmailAndStatus(
                requesterEmail = "alice@example.com",
                friendEmail = "alice@example.com",
                status = FriendshipStatus.ACCEPTED
            )
        } returns listOf(friendship1, friendship2)

        val result = service.getUserFriends("alice@example.com")

        assertTrue(result.contains("bob@example.com"))
        assertEquals(2, result.size)
    }
}
