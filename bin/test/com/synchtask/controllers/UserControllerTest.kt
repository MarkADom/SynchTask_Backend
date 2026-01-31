package com.synchtask.controllers

import com.synchtask.dtos.user.UpdateUserDTO
import com.synchtask.dtos.user.UserRegistrationDTO
import com.synchtask.dtos.user.UserResponseDTO
import com.synchtask.dtos.user.UserStatusDTO
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.services.user.UserService
import io.mockk.*
import org.junit.jupiter.api.*
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest {

    private lateinit var userService: UserService
    private lateinit var passwordEncoder: BCryptPasswordEncoder
    private lateinit var controller: UserController

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        userService = mockk()
        passwordEncoder = mockk()
        controller = UserController(userService, passwordEncoder)
    }

    @Test
    fun `should create user`() {
        val dto = UserRegistrationDTO("John", "john@email.com", "password", null)
        val encodedPassword = "encoded-password"
        val user = User(id = 1L, name = "John", email = "john@email.com", passwordHash = encodedPassword)

        every { passwordEncoder.encode("password") } returns encodedPassword
        every { userService.createUser(any()) } returns user

        val response = controller.createUser(dto)

        Assertions.assertEquals(HttpStatus.CREATED, response.statusCode)
        Assertions.assertEquals(dto.email, response.body!!.email)
        verify { userService.createUser(any()) }
    }

    @Test
    fun `should get user by id`() {
        val user = User(id = 42L, name = "Alice", email = "alice@email.com", passwordHash = "pw", role = UserRole.USER)
        every { userService.getUserById(42L) } returns user

        val response = controller.getUser(42L)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals("Alice", response.body?.name)
    }

    @Test
    fun `should return 404 if user not found`() {
        every { userService.getUserById(99L) } returns null

        val exception = assertThrows<ResourceNotFoundException> {
            controller.getUser(99L)
        }

        Assertions.assertEquals("User not found with ID: 99", exception.message)
    }

    @Test
    fun `should get all users`() {
        val users = listOf(
            UserResponseDTO(1L, "User1", "u1@email.com", "pic"),
            UserResponseDTO(2L, "User2", "u2@email.com", "pic")
        )
        every { userService.getAllUsers() } returns users

        val response = controller.getAllUsers()

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(2, response.body!!.size)
    }

    @Test
    fun `should update user`() {
        val id = 1L
        val updateDTO = UpdateUserDTO("Updated", "updated@email.com", "pic", null)
        val oldUser = User(id = id, name = "Old", email = "old@email.com", passwordHash = "pw")
        val updatedUser = updateDTO.toUser(oldUser)

        mockkStatic(SecurityContextHolder::class)
        val auth = UsernamePasswordAuthenticationToken("updated@email.com", null)
        every { SecurityContextHolder.getContext().authentication } returns auth

        every { userService.getUserById(id) } returns oldUser
        every { userService.isAuthorized("updated@email.com", oldUser.email) } returns true
        every { userService.updateUser(id, any()) } returns updatedUser

        val response = controller.updateUser(id, updateDTO)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(updateDTO.email, response.body!!.email)

        unmockkStatic(SecurityContextHolder::class)
    }

    @Test
    fun `should forbid update if unauthorized`() {
        val id = 1L
        val updateDTO = UpdateUserDTO("Updated", "updated@email.com", null, null)
        val user = User(id = id, name = "User", email = "user@email.com", passwordHash = "pw")

        mockkStatic(SecurityContextHolder::class)
        val auth = UsernamePasswordAuthenticationToken("attacker@email.com", null)
        every { SecurityContextHolder.getContext().authentication } returns auth

        every { userService.getUserById(id) } returns user
        every { userService.isAuthorized("attacker@email.com", user.email) } returns false

        val response = controller.updateUser(id, updateDTO)
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode)

        unmockkStatic(SecurityContextHolder::class)
    }

    @Test
    fun `should delete user if authorized`() {
        val user = User(id = 3L, name = "Del", email = "del@email.com", passwordHash = "pw")

        mockkStatic(SecurityContextHolder::class)
        val auth = UsernamePasswordAuthenticationToken(user.email, null)
        every { SecurityContextHolder.getContext().authentication } returns auth

        every { userService.getUserById(user.id!!) } returns user
        every { userService.isAuthorized(user.email, user.email) } returns true
        every { userService.deleteUser(user.id!!) } just Runs

        val response = controller.deleteUser(user.id!!)
        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.statusCode)

        unmockkStatic(SecurityContextHolder::class)
    }

    @Test
    fun `should throw exception if delete unauthorized`() {
        val user = User(id = 10L, name = "Bob", email = "bob@email.com", passwordHash = "pw")

        mockkStatic(SecurityContextHolder::class)
        val auth = UsernamePasswordAuthenticationToken("hacker@email.com", null)
        every { SecurityContextHolder.getContext().authentication } returns auth

        every { userService.getUserById(user.id!!) } returns user
        every { userService.isAuthorized("hacker@email.com", user.email) } returns false

        val exception = assertThrows<com.synchtask.exception.UnauthorizedAccessException> {
            controller.deleteUser(user.id!!)
        }

        Assertions.assertEquals("Unauthorized to delete this user", exception.message)

        unmockkStatic(SecurityContextHolder::class)
    }

    @Test
    fun `should get online users`() {
        val onlineUsers = listOf(
            UserStatusDTO("a@email.com", "A", now),
            UserStatusDTO("b@email.com", "B", now)
        )

        every { userService.getOnlineUsers() } returns onlineUsers

        val response = controller.getOnlineUsers()
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(2, response.body!!.size)
    }
}
