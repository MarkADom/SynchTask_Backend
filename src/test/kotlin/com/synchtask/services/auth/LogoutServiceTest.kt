
package com.synchtask.services.auth

import com.synchtask.security.application.service.LogoutService
import com.synchtask.security.domain.entity.RefreshToken
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.security.domain.repository.RefreshTokenRepository
import io.mockk.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.util.*

class LogoutServiceTest {

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var logoutService: LogoutService
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var writer: StringWriter

    @BeforeEach
    fun setup() {
        refreshTokenRepository = mockk()
        logoutService = LogoutService(refreshTokenRepository)
        request = mockk()
        response = mockk()
        writer = StringWriter()
        every { response.writer } returns PrintWriter(writer)
    }

    @Test
    fun `should revoke token and return success`() {
        val token = "valid-token"
        val user = User(id = 1L, name = "John", email = "john@email.com", passwordHash = "hash", role = UserRole.USER)
        val refreshToken = RefreshToken(id = 1L, token = token, user = user, expiryDate = LocalDateTime.now().plusDays(7), isRevoked = false)

        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { refreshTokenRepository.findByToken(token) } returns Optional.of(refreshToken)
        every { refreshTokenRepository.save(any()) } returns refreshToken
        every { response.status = any() } just Runs

        logoutService.logout(request, response, mockk<Authentication>())

        assert(writer.toString().contains("Logout successful"))
        verify { refreshTokenRepository.save(match { it.isRevoked }) }
        verify { response.status = HttpServletResponse.SC_OK }
    }

    @Test
    fun `should return bad request if token is missing`() {
        every { request.getHeader("Authorization") } returns null
        every { response.status = any() } just Runs

        logoutService.logout(request, response, null)

        assert(writer.toString().contains("No token provided"))
        verify { response.status = HttpServletResponse.SC_BAD_REQUEST }
    }

    @Test
    fun `should handle non-existent token gracefully`() {
        val token = "nonexistent-token"
        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { refreshTokenRepository.findByToken(token) } returns Optional.empty()
        every { response.status = any() } just Runs

        logoutService.logout(request, response, null)

        assert(writer.toString().contains("Logout successful"))
        verify { response.status = HttpServletResponse.SC_OK }
    }
}
