package com.synchtask.controllers

import com.synchtask.config.TestSecurityConfig
import com.synchtask.dtos.user.UserLoginDTO
import com.synchtask.dtos.user.UserRegistrationDTO
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.managers.AuthManager
import com.synchtask.security.JwtKeyManager
import com.synchtask.services.auth.AuthService
import com.synchtask.services.auth.RefreshTokenService
import com.synchtask.services.user.UserService
import io.mockk.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@Import(TestSecurityConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        val expectedTokens = mapOf("accessToken" to "jwt-access-token", "refreshToken" to "jwt-refresh-token")

        every { authManager.authenticateUser(loginDto.email, loginDto.password) } returns expectedTokens

        val response = authController.login(loginDto)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expectedTokens, response.body)
        verify { authManager.authenticateUser(loginDto.email, loginDto.password) }
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
    fun `should revoke refresh tokens and logout user`() {
        val request = mockk<HttpServletRequest>(relaxed = true)
        val response = mockk<HttpServletResponse>(relaxed = true)

        val email = "ana@example.com"
        val authentication = mockk<Authentication> {
            every { name } returns email
        }

        val user = User(id = 1L, name = "Ana", email = email, passwordHash = "hashed")

        mockkStatic(SecurityContextHolder::class)

        val context = mockk<SecurityContext>()
        every { context.authentication } returns authentication
        every { SecurityContextHolder.getContext() } returns context

        every { userService.getUserByEmail(email) } returns user
        every { refreshTokenService.revokeTokensForUser(user) } just Runs

        authController.logout(request, response)

        verify { userService.getUserByEmail(email) }
        verify { refreshTokenService.revokeTokensForUser(user) }

        unmockkStatic(SecurityContextHolder::class)
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
