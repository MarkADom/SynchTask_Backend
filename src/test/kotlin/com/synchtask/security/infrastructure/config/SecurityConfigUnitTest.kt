package com.synchtask.security.infrastructure.config

import com.synchtask.security.infrastructure.filter.RateLimitFilter
import io.mockk.mockk
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt

class SecurityConfigUnitTest {
    private val rateLimitFilter = mockk<RateLimitFilter>(relaxed = true)
    private val securityConfig = SecurityConfig(rateLimitFilter)

    @Test
    fun `passwordEncoder encodes values`() {
        val encoded = securityConfig.passwordEncoder().encode("secret")
        assertTrue(encoded.isNotBlank())
        assertTrue(securityConfig.passwordEncoder().matches("secret", encoded))
    }

    @Test
    fun `jwtAuthenticationConverter builds authentication`() {
        val uds = UserDetailsService { username -> User(username, "pwd", emptyList()) }
        val converter = securityConfig.jwtAuthenticationConverter(uds)
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "john@test.com")
            .claim("roles", listOf("USER"))
            .build()

        val auth = converter.convert(jwt)

        assertEquals("john@test.com", auth?.name)
        assertEquals("ROLE_USER", auth?.authorities?.first()?.authority)
    }

    @Test
    fun `logoutSuccessHandler returns success payload`() {
        val response = mockk<HttpServletResponse>(relaxed = true)
        val writer = java.io.PrintWriter(java.io.StringWriter())
        io.mockk.every { response.writer } returns writer

        securityConfig.logoutSuccessHandler().onLogoutSuccess(null, response, null)

        io.mockk.verify { response.status = HttpServletResponse.SC_OK }
    }
}
