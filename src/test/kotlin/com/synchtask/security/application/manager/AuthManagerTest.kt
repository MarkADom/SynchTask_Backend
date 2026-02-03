package com.synchtask.security.application.manager

import com.synchtask.security.application.context.AuthServiceContext
import com.synchtask.security.application.service.AuthService
import com.synchtask.security.application.service.RefreshTokenService
import com.synchtask.security.domain.entity.RefreshToken
import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AuthManagerTest {

    private lateinit var userService: UserService
    private lateinit var authService: AuthService
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var manager: AuthManager

    @BeforeEach
    fun setup() {
        userService = mockk()
        authService = mockk()
        refreshTokenService = mockk()
        jwtTokenProvider = mockk()
        userDetailsService = mockk()
        passwordEncoder = mockk()

        val context = AuthServiceContext(
            authService,
            refreshTokenService,
            userDetailsService,
            jwtTokenProvider,
            userService,
            passwordEncoder
        )

        manager = AuthManager(context)
    }

    @Test
    fun `should register user via UserService`() {
        val dto = UserRegistrationDTO("Alice", "alice@example.com", "password")
        val encodedPassword = "encoded123"
        val user = User(id = 1L, name = "Alice", email = "alice@example.com", passwordHash = encodedPassword)

        every { passwordEncoder.encode("password") } returns encodedPassword
        every { userService.createUser(any()) } returns user

        val result = manager.registerUser(dto)

        assertEquals("Alice", result.name)
        assertEquals("alice@example.com", result.email)
        verify { userService.createUser(any()) }
    }

    @Test
    fun `should authenticate user via AuthService`() {
        val expectedTokens = mapOf("accessToken" to "abc", "refreshToken" to "xyz")

        every { authService.authenticate("bob@example.com", "secret") } returns expectedTokens

        val result = manager.authenticateUser("bob@example.com", "secret")

        assertEquals(expectedTokens, result)
        verify { authService.authenticate("bob@example.com", "secret") }
    }

    @Test
    fun `should refresh token via RefreshTokenService and return JWT`() {
        val user = User(
            id = 2L,
            name = "Jane",
            email = "jane@example.com",
            passwordHash = "hashed"
        )

        val refreshToken = RefreshToken(
            id = 1L,
            user = user,
            token = "valid-refresh-token",
            expiryDate = LocalDateTime.now().plusDays(7),
            isRevoked = false
        )

        val userDetails = mockk<UserDetails> {
            every { username } returns "jane@example.com"
        }

        every { refreshTokenService.validateRefreshToken("valid-refresh-token") } returns refreshToken
        every { userDetailsService.loadUserByUsername("jane@example.com") } returns userDetails
        every { jwtTokenProvider.generateToken(userDetails) } returns "new.jwt.token"

        val result = manager.refreshJwt("valid-refresh-token")

        assertEquals("new.jwt.token", result)
        verify { refreshTokenService.validateRefreshToken("valid-refresh-token") }
        verify { jwtTokenProvider.generateToken(userDetails) }
    }

    @Test
    fun `should revoke tokens on logout if user exists`() {
        val user = User(id = 3L, name = "Bob", email = "bob@example.com", passwordHash = "x")
        every { userService.getUserByEmail("bob@example.com") } returns user
        every { refreshTokenService.revokeTokensForUser(user) } just Runs

        manager.logoutUser("bob@example.com")

        verify { refreshTokenService.revokeTokensForUser(user) }
    }

    @Test
    fun `should not revoke tokens on logout if user not found`() {
        every { userService.getUserByEmail("ghost@example.com") } returns null

        manager.logoutUser("ghost@example.com")

        verify(exactly = 0) { refreshTokenService.revokeTokensForUser(any()) }
    }
}
