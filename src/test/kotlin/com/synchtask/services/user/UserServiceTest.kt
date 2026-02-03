package com.synchtask.services.user

import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.application.dto.UserStatusDTO
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.exception.UserAlreadyExistsException
import com.synchtask.user.presentation.mapper.UserMapper
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.*
import kotlin.test.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: BCryptPasswordEncoder
    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        userRepository = mockk(relaxed = true)
        passwordEncoder = mockk(relaxed = true)
        userService = UserService(userRepository, passwordEncoder)
        clearMocks(userRepository, passwordEncoder)
    }

    private fun buildUser(
        id: Long = 1L,
        email: String = "test@email.com",
        name: String = "Test",
        passwordHash: String = "hashed",
        role: UserRole = UserRole.USER,
        isOnline: Boolean = false,
        lastActivity: LocalDateTime? = null
    ): User {
        return User(
            id = id,
            name = name,
            email = email,
            passwordHash = passwordHash,
            role = role,
            isOnline = isOnline,
            lastActivity = lastActivity
        )
    }

    @Test
    fun `should create user and encode password`() {
        val rawPassword = "password"
        every { passwordEncoder.encode(rawPassword) } returns "encoded"
        every { userRepository.findByEmail(any()) } returns Optional.empty()
        every { userRepository.save(any()) } answers { firstArg() }

        val newUser = buildUser(passwordHash = rawPassword)

        val result = userService.createUser(newUser)

        assertEquals("encoded", result.passwordHash)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should skip encoding if password already hashed`() {
        val user = buildUser(passwordHash = "\$2a\$hashed")
        every { userRepository.findByEmail(any()) } returns Optional.empty()
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.createUser(user)

        assertEquals("\$2a\$hashed", result.passwordHash)
        verify { passwordEncoder wasNot Called }
    }

    @Test
    fun `should throw if email already exists`() {
        val user = buildUser()
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)

        assertFailsWith<UserAlreadyExistsException> {
            userService.createUser(user)
        }
    }

    @Test
    fun `should update user data and hash new password`() {
        val existing = buildUser()
        val updates = existing.copy(name = "New Name", passwordHash = "newpass")

        every { userRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { passwordEncoder.encode("newpass") } returns "new-hash"
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.updateUser(existing.id!!, updates)

        assertEquals("New Name", result?.name)
        assertEquals("new-hash", result?.passwordHash)
    }

    @Test
    fun `should not hash password again on update if already hashed`() {
        val existing = buildUser(passwordHash = "old-value-will-be-kept")
        val updates = existing.copy(passwordHash = "\$2a\$existinghash")

        every { userRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.updateUser(existing.id!!, updates)

        assertEquals("old-value-will-be-kept", result?.passwordHash)
        verify(exactly = 0) { passwordEncoder.encode(any()) }
    }


    @Test
    fun `should return user by ID or null`() {
        val user = buildUser()
        every { userRepository.findById(1L) } returns Optional.of(user)

        assertEquals(user.email, userService.getUserById(1L)?.email)

        every { userRepository.findById(2L) } returns Optional.empty()
        assertNull(userService.getUserById(2L))
    }

    @Test
    fun `should return user by email or null`() {
        val user = buildUser()
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)

        assertEquals(user.email, userService.getUserByEmail(user.email)?.email)

        every { userRepository.findByEmail("none") } returns Optional.empty()
        assertNull(userService.getUserByEmail("none"))
    }

    @Test
    fun `should delete user by ID`() {
        every { userRepository.deleteById(1L) } just Runs
        userService.deleteUser(1L)
        verify { userRepository.deleteById(1L) }
    }

    @Test
    fun `should check if user is admin`() {
        val admin = buildUser(role = UserRole.ADMIN)
        every { userRepository.findByEmail(admin.email) } returns Optional.of(admin)

        assertTrue(userService.isAdmin(admin.email))
    }

    @Test
    fun `should check if user is authorized`() {
        val admin = buildUser(role = UserRole.ADMIN)
        every { userRepository.findByEmail(admin.email) } returns Optional.of(admin)

        assertTrue(userService.isAuthorized(admin.email, "target"))
        assertTrue(userService.isAuthorized(admin.email, admin.email))
    }

    @Test
    fun `should update user online status and timestamp`() {
        val user = buildUser(isOnline = false)
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }

        userService.setUserOnlineStatus(user.email, true)
        verify { userRepository.save(match { it.isOnline }) }

        userService.setUserOnlineStatus(user.email, false)
        verify { userRepository.save(match { !it.isOnline && it.lastActivity != null }) }
    }

    @Test
    fun `should update last activity timestamp`() {
        val user = buildUser()
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }

        userService.updateLastActivity(user.email)

        verify {
            userRepository.save(match { it.lastActivity != null })
        }
    }

    @Test
    fun `should check if user is online`() {
        val user = buildUser(isOnline = true)
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)

        assertTrue(userService.isUserOnline(user.email))

        every { userRepository.findByEmail("ghost") } returns Optional.empty()
        assertFalse(userService.isUserOnline("ghost"))
    }

    @Test
    fun `should return online users`() {
        val online = buildUser(isOnline = true, lastActivity = LocalDateTime.now())
        every { userRepository.findAllByIsOnlineTrue() } returns listOf(online)

        val result = userService.getOnlineUsers()

        assertEquals(1, result.size)
        assertTrue(result[0] is UserStatusDTO)
    }

    @Test
    fun `should return all users as UserResponseDTO`() {
        val user = buildUser()
        every { userRepository.findAll() } returns listOf(user)
        mockkObject(UserMapper)
        every { UserMapper.toResponseDTO(user) } returns UserResponseDTO(
            id = user.id!!,
            name = user.name,
            email = user.email,
            profilePictureUrl = "N/A"
        )

        val result = userService.getAllUsers()

        assertEquals(1, result.size)
        verify { UserMapper.toResponseDTO(user) }
    }
}
