package com.synchtask.security.presentation.controller

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * **OpenIdConfigurationControllerTest**
 *
 * Unit test for OpenID Connect discovery endpoint.
 */
class OpenIdConfigurationControllerTest {
    private val controller = OpenIdConfigurationController()

    @Test
    fun `should return correct OpenID configuration`() {
        val request = mockk<HttpServletRequest>()

        every { request.scheme } returns "https"
        every { request.serverName } returns "example.com"
        every { request.serverPort } returns 443

        val result = controller.openIdConfig(request)

        val expectedBaseUrl = "https://example.com:443"

        assertEquals(expectedBaseUrl, result.issuer)
        assertEquals("$expectedBaseUrl/jwks", result.jwksUri)
        assertEquals("$expectedBaseUrl/oauth2/authorization/google", result.authorizationEndpoint)
        assertEquals("$expectedBaseUrl/auth/refresh", result.tokenEndpoint)
        assertEquals("$expectedBaseUrl/oauth2/com/synchtask/user", result.userinfoEndpoint)
    }
}
