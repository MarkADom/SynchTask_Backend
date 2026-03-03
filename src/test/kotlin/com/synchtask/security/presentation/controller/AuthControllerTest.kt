package com.synchtask.security.presentation.controller

import com.synchtask.security.application.dto.JwkKeyDTO
import com.synchtask.security.application.dto.JwksResponseDTO
import com.synchtask.security.application.dto.TokenPairDTO
import com.synchtask.security.application.manager.AuthManager
import com.synchtask.security.application.service.AuthService
import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import com.synchtask.user.application.dto.UserLoginDTO
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.application.service.AuthenticatedUserService
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.Runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

class AuthControllerTest {
    private lateinit var authManager: AuthManager
    private lateinit var jwtKeyManager: JwtKeyManager
    private lateinit var userService: UserService
    private lateinit var authenticatedUserService: AuthenticatedUserService
    private lateinit var authService: AuthService

    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        authManager = mockk(relaxed = true)
        jwtKeyManager = mockk(relaxed = true)
        userService = mockk(relaxed = true)
        authenticatedUserService = mockk(relaxed = true)
        authService = mockk(relaxed = true)

        authController =
            AuthController(
                authManager = authManager,
                jwtKeyManager = jwtKeyManager,
                userService = userService,
                authenticatedUserService = authenticatedUserService,
                authService = authService
            )
    }

    @Test
    fun `should register new user and return UserResponseDTO`() {
        val dto = UserRegistrationDTO(name = "John Doe", email = "john@example.com", password = "1234")
        val savedUser =
            User(
                id = 1L,
                name = dto.name,
                email = dto.email,
                passwordHash = "hashed123",
                role = UserRole.USER
            )

        every { authManager.registerUser(dto) } returns savedUser

        val response = authController.registerUser(dto)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(dto.email, response.body?.email)
        verify { authManager.registerUser(dto) }
    }

    @Test
    fun `should authenticate and return structured auth response`() {
        val loginDto = UserLoginDTO(email = "test@email.com", password = "secure123")
        val tokens = TokenPairDTO(accessToken = "jwt-access-token", refreshToken = "jwt-refresh-token")

        val user =
            User(
                id = 1L,
                name = "Test User",
                email = "test@email.com",
                passwordHash = "hashed",
                role = UserRole.USER,
                profilePictureUrl = null
            )

        every { authManager.authenticateUser(loginDto.email, loginDto.password) } returns tokens
        every { userService.getUserByEmail(loginDto.email) } returns user

        val response = authController.login(loginDto)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!

        assertEquals("jwt-access-token", body.accessToken)
        assertEquals("jwt-refresh-token", body.refreshToken)
        assertEquals(user.email, body.user.email)
        assertEquals(user.name, body.user.name)

        verify {
            authManager.authenticateUser(loginDto.email, loginDto.password)
            userService.getUserByEmail(loginDto.email)
        }
    }

    @Test
    fun `should return 401 for invalid credentials`() {
        val loginDto = UserLoginDTO(email = "fake@email.com", password = "wrong")
        every { authManager.authenticateUser(any(), any()) } throws SecurityException("Invalid credentials")

        val exception = assertThrows(InvalidCredentialsException::class.java) { authController.login(loginDto) }

        assertEquals("Invalid credentials", exception.message)
        verify { authManager.authenticateUser(loginDto.email, loginDto.password) }
    }

    @Test
    fun `should return JWKS DTO`() {
        val jwksMock = JwksResponseDTO(keys = listOf(JwkKeyDTO("RSA", "RS256", "sig", "n", "e", "kid")))
        every { jwtKeyManager.getJwks() } returns jwksMock

        val result = authController.getJwks()

        assertEquals(jwksMock, result)
    }

    @Test
    fun `should return new access token using refresh token`() {
        every { authManager.refreshJwt("refresh-123") } returns "new-access-token"

        val response = authController.refresh(com.synchtask.security.application.dto.RefreshTokenRequestDTO("refresh-123"))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("new-access-token", response.body?.accessToken)
        verify { authManager.refreshJwt("refresh-123") }
    }

    @Test
    fun `should return OAuth2 user info DTO`() {
        val principal = mockk<OAuth2User>()
        every { principal.attributes } returns mapOf("email" to "oauth@example.com", "name" to "OAuth")
        every { principal.authorities } returns listOf()

        val response = authController.getOAuth2User(principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("oauth@example.com", response.body?.email)
        assertEquals("OAuth", response.body?.name)
    }

    @Test
    fun `should return OIDC user info DTO`() {
        val oidcUser = mockk<OidcUser>()

        every { oidcUser.email } returns "oidc@example.com"
        every { oidcUser.fullName } returns "OIDC User"
        every { oidcUser.subject } returns "sub"
        every { oidcUser.authorities } returns listOf()

        val response = authController.getOidcUser(oidcUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("oidc@example.com", response.body?.email)
        assertEquals("OIDC User", response.body?.name)
    }

    @Test
    fun `should update user role as admin`() {
        val adminUser = mockk<UserDetails>()
        every { adminUser.username } returns "admin@example.com"
        every { authService.updateUserRole("admin@example.com", 42L, UserRole.OWNER) } just Runs

        val response = authController.updateUserRole(adminUser, 42L, UserRole.OWNER)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("User role updated successfully", response.body?.message)

        verify { authService.updateUserRole("admin@example.com", 42L, UserRole.OWNER) }
    }
}
