package com.synchtask.user.presentation.controller

import com.synchtask.friend.application.service.FriendService
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.application.dto.*
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserControllerTest {

    private lateinit var userService: UserService
    private lateinit var passwordEncoder: BCryptPasswordEncoder
    private lateinit var friendService: FriendService
    private lateinit var controller: UserController

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        clearAllMocks()
        userService = mockk()
        passwordEncoder = mockk()
        friendService = mockk()
        controller = UserController(userService, passwordEncoder, friendService)
    }

    @Test
    fun `should create user`() {
        val dto = UserRegistrationDTO("John", "john@email.com", "password", null)
        val encoded = "encoded"

        val createdUser = User(
            id = 1L,
            name = "John",
            email = dto.email,
            passwordHash = encoded,
            role = UserRole.USER
        )

        every { passwordEncoder.encode(dto.password) } returns encoded
        every { userService.createUser(any()) } returns createdUser

        val response = controller.createUser(dto)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(dto.email, response.body!!.email)
    }

    @Test
    fun `should get user by id`() {
        val user = User(
            id = 42L,
            name = "Alice",
            email = "alice@email.com",
            passwordHash = "pw",
            role = UserRole.USER
        )

        every { userService.getUserById(42L) } returns user

        val response = controller.getUser(42L)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Alice", response.body!!.name)
    }

    @Test
    fun `should throw 404 if user not found`() {
        every { userService.getUserById(99L) } returns null

        val ex = assertFailsWith<ResourceNotFoundException> {
            controller.getUser(99L)
        }

        assertEquals("User not found with ID: 99", ex.message)
    }

    @Test
    fun `should get all users`() {
        val users = listOf(
            UserResponseDTO(1L, "User1", "u1@email.com", null),
            UserResponseDTO(2L, "User2", "u2@email.com", null)
        )

        every { userService.getAllUsers() } returns users

        val response = controller.getAllUsers()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body!!.size)
    }

    @Test
    fun `should update user when authorized`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("admin@email.com", "pw", emptyList())

        val existing = User(
            id = 1L,
            name = "Old",
            email = "old@email.com",
            passwordHash = "pw",
            role = UserRole.USER
        )

        val updated = existing.copy(name = "New", email = "new@email.com")

        every { userService.getUserById(1L) } returns existing
        every { userService.isAuthorized(authUser.username, existing.email) } returns true
        every { userService.updateUser(1L, any()) } returns updated

        val response = controller.updateUser(
            1L,
            UpdateUserDTO("New", "new@email.com", null, null),
            authUser
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("new@email.com", response.body!!.email)
    }

    @Test
    fun `should forbid update if unauthorized`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("attacker@email.com", "pw", emptyList())

        val existing = User(
            id = 1L,
            name = "User",
            email = "user@email.com",
            passwordHash = "pw"
        )

        every { userService.getUserById(1L) } returns existing
        every { userService.isAuthorized(authUser.username, existing.email) } returns false

        val response = controller.updateUser(
            1L,
            UpdateUserDTO("X", "x@email.com", null, null),
            authUser
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `should return 404 when update user target does not exist`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("admin@email.com", "pw", emptyList())

        every { userService.getUserById(999L) } returns null

        val response = controller.updateUser(
            999L,
            UpdateUserDTO("X", "x@email.com", null, null),
            authUser
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should delete user if authorized`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("user@email.com", "pw", emptyList())

        val user = User(
            id = 3L,
            name = "Del",
            email = "user@email.com",
            passwordHash = "pw"
        )

        every { userService.getUserById(3L) } returns user
        every { userService.isAuthorized(authUser.username, user.email) } returns true
        every { userService.deleteUser(3L) } just Runs

        val response = controller.deleteUser(3L, authUser)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
    }

    @Test
    fun `should throw if delete unauthorized`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("hacker@email.com", "pw", emptyList())

        val user = User(
            id = 10L,
            name = "Bob",
            email = "bob@email.com",
            passwordHash = "pw"
        )

        every { userService.getUserById(10L) } returns user
        every { userService.isAuthorized(authUser.username, user.email) } returns false

        val ex = assertFailsWith<UnauthorizedAccessException> {
            controller.deleteUser(10L, authUser)
        }

        assertEquals("Unauthorized to delete this user", ex.message)
    }

    @Test
    fun `should update current user`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("me@email.com", "pw", emptyList())

        val existing = User(
            id = 1L,
            name = "Old",
            email = "me@email.com",
            passwordHash = "pw"
        )

        val updated = existing.copy(name = "New")

        every { userService.getUserByEmail(authUser.username) } returns existing
        every { userService.updateUser(1L, any()) } returns updated

        val response = controller.updateCurrentUser(
            UpdateUserDTO("New", "user@email.com", null, null),
            authUser
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("New", response.body!!.name)
    }

    @Test
    fun `should throw when authenticated user not found on updateCurrentUser`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("ghost@email.com", "pw", emptyList())

        every { userService.getUserByEmail(authUser.username) } returns null

        val ex = assertFailsWith<ResourceNotFoundException> {
            controller.updateCurrentUser(UpdateUserDTO("X", "x@email.com", null, null), authUser)
        }

        assertEquals("Authenticated user not found", ex.message)
    }

    @Test
    fun `should get online users`() {
        val onlineUsers = listOf(
            UserStatusDTO("a@email.com", "A", now),
            UserStatusDTO("b@email.com", "B", now)
        )

        every { userService.getOnlineUsers() } returns onlineUsers

        val response = controller.getOnlineUsers()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body!!.size)
    }

    @Test
    fun `should get visible users`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("me@email.com", "pw", emptyList())

        val current = User(
            id = 1L,
            name = "Me",
            email = "me@email.com",
            passwordHash = "pw"
        )

        val visibleDto = UserResponseDTO(
            id = 2L,
            name = "Friend",
            email = "friend@email.com",
            profilePictureUrl = null
        )

        every { userService.getUserByEmail(authUser.username) } returns current
        every { userService.getVisibleUsers(current, emptyList()) } returns listOf(visibleDto)

        val response = controller.getVisibleUsers(authUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.size)
        assertEquals("Friend", response.body!![0].name)
    }

    @Test
    fun `should return current authenticated user`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("me@email.com", "pw", emptyList())

        val existing = User(
            id = 1L,
            name = "Me",
            email = "me@email.com",
            passwordHash = "pw"
        )

        every { userService.getUserByEmail(authUser.username) } returns existing

        val response = controller.getCurrentUser(authUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Me", response.body!!.name)
    }

    @Test
    fun `should throw when current authenticated user not found`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("ghost@email.com", "pw", emptyList())

        every { userService.getUserByEmail(authUser.username) } returns null

        val ex = assertFailsWith<ResourceNotFoundException> {
            controller.getCurrentUser(authUser)
        }

        assertEquals("Authenticated user not found", ex.message)
    }

    @Test
    fun `should return paged public users`() {
        val pageable = PageRequest.of(0, 10)
        val users = listOf(
            User(
                id = 1L,
                name = "Public One",
                email = "public1@email.com",
                passwordHash = "pw",
                isOnline = true
            ),
            User(
                id = 2L,
                name = "Public Two",
                email = "public2@email.com",
                passwordHash = "pw",
                isOnline = true
            )
        )

        every { userService.findPublicUsers("Pub", true, pageable) } returns
                PageImpl(users, pageable, users.size.toLong())

        val response = controller.getPublicUsers("Pub", true, pageable)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body!!.content.size)
        assertEquals("Public One", response.body!!.content.first().name)
    }

    @Test
    fun `should update profile picture`() {
        val authUser: UserDetails =
            org.springframework.security.core.userdetails.User("me@email.com", "pw", emptyList())
        val file = mockk<MultipartFile>()

        val updatedUser = User(
            id = 1L,
            name = "Me",
            email = "me@email.com",
            passwordHash = "pw",
            profilePictureUrl = "https://cdn/img.png"
        )

        every { userService.updateProfilePicture(authUser.username, file) } returns updatedUser

        val response = controller.updateProfilePicture(file, authUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("https://cdn/img.png", response.body!!["profilePictureUrl"])
    }

    @Test
    fun `should get assignable users`() {
        val users = listOf(
            User(
                id = 1L,
                name = "A",
                email = "a@email.com",
                passwordHash = "pw"
            ),
            User(
                id = 2L,
                name = "B",
                email = "b@email.com",
                passwordHash = "pw"
            )
        )

        every { userService.getAssignableUsers() } returns users

        val response = controller.getAssignableUsers()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body!!.size)
        assertEquals("A", response.body!![0].name)
        assertEquals("B", response.body!![1].name)
    }
}
