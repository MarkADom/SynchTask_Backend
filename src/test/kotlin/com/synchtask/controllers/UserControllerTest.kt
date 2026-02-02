package com.synchtask.controllers

import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.services.friend.FriendService
import com.synchtask.user.application.service.UserService
import com.synchtask.user.application.dto.UpdateUserDTO
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.application.dto.UserStatusDTO
import com.synchtask.user.presentation.controller.UserController
import io.mockk.*
import org.junit.jupiter.api.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest {

    private lateinit var userService: UserService
    private lateinit var passwordEncoder: BCryptPasswordEncoder
    private lateinit var friendService: FriendService
    private lateinit var controller: UserController

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        userService = mockk()
        passwordEncoder = mockk()
        friendService = mockk()
        controller = UserController(userService, passwordEncoder, friendService)
    }

    @Test
    fun `should create user`() {
        val dto = UserRegistrationDTO("John", "john@email.com", "password", null)
        val encoded = "encoded-password"

        val user = User(
            id = 1L,
            name = "John",
            email = dto.email,
            passwordHash = encoded,
            role = UserRole.USER
        )

        every { passwordEncoder.encode(dto.password) } returns encoded
        every { userService.createUser(any()) } returns user

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
            UserResponseDTO(1L, "User1", "u1@email.com", "pic"),
            UserResponseDTO(2L, "User2", "u2@email.com", "pic")
        )

        every { userService.getAllUsers() } returns users

        val response = controller.getAllUsers()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body!!.size)
    }

    @Test
    fun `should update user when authorized`() {
        val id = 1L
        val authUser: UserDetails = org.springframework.security.core.userdetails.User(
            "admin@email.com", "pw", emptyList()
        )

        val existing = User(
            id = id,
            name = "Old",
            email = "old@email.com",
            passwordHash = "pw",
            role = UserRole.USER
        )

        val updateDTO = UpdateUserDTO("New", "new@email.com", null, null)
        val updated = updateDTO.toUser(existing)

        every { userService.getUserById(id) } returns existing
        every { userService.isAuthorized(authUser.username, existing.email) } returns true
        every { userService.updateUser(id, any()) } returns updated

        val response = controller.updateUser(id, updateDTO, authUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("new@email.com", response.body!!.email)
    }

    @Test
    fun `should forbid update if unauthorized`() {
        val authUser: UserDetails = org.springframework.security.core.userdetails.User(
            "attacker@email.com", "pw", emptyList()
        )

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
    fun `should delete user if authorized`() {
        val authUser: UserDetails = org.springframework.security.core.userdetails.User(
            "user@email.com", "pw", emptyList()
        )

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
        val authUser: UserDetails = org.springframework.security.core.userdetails.User(
            "hacker@email.com", "pw", emptyList()
        )

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
}
