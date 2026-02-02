package com.synchtask.controllers

import com.synchtask.user.application.dto.UserLoginDTO
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.managers.AuthManager
import com.synchtask.security.JwtKeyManager
import com.synchtask.services.auth.AuthService
import com.synchtask.services.auth.RefreshTokenService
import com.synchtask.user.application.service.UserService
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.server.ResponseStatusException


class AuthControllerTest {

    private lateinit var authManager: AuthManager
    private lateinit var jwtKeyManager: JwtKeyManager
    private lateinit var userService: UserService
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var authService: AuthService

    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        authManager = mockk(relaxed = true)
        jwtKeyManager = mockk(relaxed = true)
        userService = mockk(relaxed = true)
        refreshTokenService = mockk(relaxed = true)
        authService = mockk(relaxed = true)

        authController = AuthController(
            authManager = authManager,
            jwtKeyManager = jwtKeyManager,
            userService = userService,
            refreshTokenService = refreshTokenService,
            authService = authService
        )
    }

    @Test
    fun `should register new user and return UserResponseDTO`() {
        val dto = UserRegistrationDTO(name = "John Doe", email = "john@example.com", password = "1234")
        val savedUser = User(id = 1L, name = dto.name, email = dto.email, passwordHash = "hashed123", role = UserRole.USER)

        every { authManager.registerUser(dto) } returns savedUser

        val response = authController.registerUser(dto)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(dto.email, response.body?.email)
        verify { authManager.registerUser(dto) }
    }

    @Test
    fun `should authenticate and return JWT token`() {
        val loginDto = UserLoginDTO(email = "test@email.com", password = "secure123")

        val tokens = mapOf(
            "accessToken" to "jwt-access-token",
            "refreshToken" to "jwt-refresh-token"
        )

        val user = User(
            id = 1L,
            name = "Test User",
            email = "test@email.com",
            passwordHash = "hashed",
            role = UserRole.USER,
            profilePictureUrl = null
        )

        every {
            authManager.authenticateUser(loginDto.email, loginDto.password)
        } returns tokens

        every {
            userService.getUserByEmail(loginDto.email)
        } returns user

        val response = authController.login(loginDto)

        assertEquals(HttpStatus.OK, response.statusCode)

        val body = response.body!!
        assertEquals("jwt-access-token", body["accessToken"])
        assertNotNull(body["com/synchtask/user"])

        val userDto = body["com/synchtask/user"] as UserResponseDTO
        assertEquals(user.email, userDto.email)
        assertEquals(user.name, userDto.name)

        verify {
            authManager.authenticateUser(loginDto.email, loginDto.password)
            userService.getUserByEmail(loginDto.email)
        }
    }

    @Test
    fun `should return 401 for invalid credentials`() {
        val loginDto = UserLoginDTO(email = "fake@email.com", password = "wrong")
        every { authManager.authenticateUser(any(), any()) } throws SecurityException("Invalid credentials")

        val exception = assertThrows<ResponseStatusException> {
            authController.login(loginDto)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
        assertEquals("Invalid credentials", exception.reason)
        verify { authManager.authenticateUser(loginDto.email, loginDto.password) }
    }

    @Test
    fun `should return JWKS`() {
        val jwksMock = mapOf("keys" to listOf(mapOf("kty" to "RSA", "kid" to "key1")))
        every { jwtKeyManager.getJwks() } returns jwksMock

        val result = authController.getJwks()

        assertEquals(jwksMock, result)
        verify { jwtKeyManager.getJwks() }
    }

    @Test
    fun `should return new access token using refresh token`() {
        val tokenMap = mapOf("refreshToken" to "refresh-123")
        val newAccessToken = "new-access-token"

        every { authManager.refreshJwt("refresh-123") } returns newAccessToken

        val response = authController.refresh(tokenMap)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("new-access-token", response.body?.get("accessToken"))
        verify { authManager.refreshJwt("refresh-123") }
    }

    @Test
    fun `should return OAuth2 user attributes`() {
        val principal = mockk<OAuth2User>()
        every { principal.attributes } returns mapOf("email" to "oauth@example.com")

        val response = authController.getOAuth2User(principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("oauth@example.com", response.body?.get("email"))
    }

    @Test
    fun `should return OIDC user claims`() {
        val oidcUser = mockk<OidcUser>()
        every { oidcUser.email } returns "oidc@example.com"
        every { oidcUser.claims } returns mapOf("email" to "oidc@example.com")

        val response = authController.getOidcUser(oidcUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("oidc@example.com", response.body?.get("email"))
    }

    @Test
    fun `should update user role as admin`() {
        val adminUser = mockk<UserDetails>()
        every { adminUser.username } returns "admin@example.com"
        every { authService.updateUserRole("admin@example.com", 42L, UserRole.OWNER) } just Runs

        val response = authController.updateUserRole(adminUser, 42L, UserRole.OWNER)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("User role updated successfully", response.body)

        verify { authService.updateUserRole("admin@example.com", 42L, UserRole.OWNER) }
    }
}
