package com.synchtask.services.auth

import com.synchtask.entities.RefreshToken
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.exception.InvalidCredentialsException
import com.synchtask.repositories.UserRepository
import com.synchtask.security.JwtTokenProvider
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthServiceTest {

    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authService: AuthService

    @BeforeAll
    fun setup() {
        authenticationManager = mockk()
        userDetailsService = mockk()
        jwtTokenProvider = mockk()
        refreshTokenService = mockk()
        userRepository = mockk()
        passwordEncoder = mockk()

        authService = AuthService(
            authenticationManager,
            userDetailsService,
            jwtTokenProvider,
            refreshTokenService,
            userRepository,
            passwordEncoder
        )
    }

    @Test
    fun `should authenticate and return tokens`() {
        val email = "user@synchtask.com"
        val rawPassword = "123456"
        val user = User(
            id = 1L,
            name = "Test User",
            email = email,
            passwordHash = "hashed",
            profilePictureUrl = "",
            role = UserRole.USER
        )
        val userDetails = mockk<UserDetails>()

        every { userDetails.username } returns email
        every { userDetails.password } returns "hashed"
        every { userDetails.authorities } returns listOf()
        every { userDetailsService.loadUserByUsername(email) } returns userDetails
        every { passwordEncoder.matches(rawPassword, "hashed") } returns true
        every { authenticationManager.authenticate(any()) } returns mockk()
        every { jwtTokenProvider.generateToken(userDetails) } returns "jwt-token"
        every { userRepository.findByEmail(email) } returns Optional.of(user)
        every { refreshTokenService.createRefreshToken(user) } returns RefreshToken(
            id = 1L,
            token = "refresh-token",
            user = user,
            expiryDate = LocalDateTime.now().plusHours(1)
        )

        val result = authService.authenticate(email, rawPassword)

        assertEquals("jwt-token", result["accessToken"])
        assertEquals("refresh-token", result["refreshToken"])
    }

    @Test
    fun `should throw exception for invalid email`() {
        val email = "invalid@email.com"
        every { userDetailsService.loadUserByUsername(email) } throws InvalidCredentialsException("Invalid email or password")

        val exception = assertThrows<InvalidCredentialsException> {
            authService.authenticate(email, "123456")
        }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `should throw exception for wrong password`() {
        val email = "user@synchtask.com"
        val userDetails = mockk<UserDetails>()

        every { userDetails.username } returns email
        every { userDetails.password } returns "hashed"
        every { userDetailsService.loadUserByUsername(email) } returns userDetails
        every { passwordEncoder.matches("wrong", "hashed") } returns false

        val exception = assertThrows<InvalidCredentialsException> {
            authService.authenticate(email, "wrong")
        }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `should update user role when performed by admin`() {
        val admin = User(
            id = 1L,
            name = "Admin",
            email = "admin@synchtask.com",
            passwordHash = "admin123",
            profilePictureUrl = "",
            role = UserRole.ADMIN
        )
        val user = User(
            id = 2L,
            name = "Target",
            email = "user@synchtask.com",
            passwordHash = "user123",
            profilePictureUrl = "",
            role = UserRole.USER
        )

        every { userRepository.findByEmail(admin.email) } returns Optional.of(admin)
        every { userRepository.findById(user.id!!) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user.copy(role = UserRole.COLLABORATOR)

        authService.updateUserRole(admin.email, user.id!!, UserRole.COLLABORATOR)

        verify { userRepository.save(match { it.role == UserRole.COLLABORATOR }) }
    }

    @Test
    fun `should throw forbidden when non-admin tries to update role`() {
        val nonAdmin = User(
            id = 1L,
            name = "User",
            email = "user@synchtask.com",
            passwordHash = "user123",
            profilePictureUrl = "",
            role = UserRole.USER
        )
        every { userRepository.findByEmail(nonAdmin.email) } returns Optional.of(nonAdmin)

        val exception = assertThrows<ResponseStatusException> {
            authService.updateUserRole(nonAdmin.email, 999L, UserRole.COLLABORATOR)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `should prevent assigning ADMIN role`() {
        val admin = User(
            id = 1L,
            name = "Admin",
            email = "admin@synchtask.com",
            passwordHash = "admin123",
            profilePictureUrl = "",
            role = UserRole.ADMIN
        )
        val targetUser = User(
            id = 2L,
            name = "Target",
            email = "target@synchtask.com",
            passwordHash = "pass123",
            profilePictureUrl = "",
            role = UserRole.USER
        )

        every { userRepository.findByEmail(admin.email) } returns Optional.of(admin)
        every { userRepository.findById(targetUser.id!!) } returns Optional.of(targetUser)

        val exception = assertThrows<ResponseStatusException> {
            authService.updateUserRole(admin.email, targetUser.id!!, UserRole.ADMIN)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }
}
