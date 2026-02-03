package com.synchtask.controllers

import com.synchtask.security.presentation.controller.OpenIdConfigurationController
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

        val expectedBaseUrl = "https://example.com:443/api/auth"

        assertEquals(expectedBaseUrl, result["issuer"])
        assertEquals("$expectedBaseUrl/api/jwks", result["jwks_uri"])
        assertEquals("$expectedBaseUrl/api/auth/login", result["authorization_endpoint"])
        assertEquals("$expectedBaseUrl/api/auth/token", result["token_endpoint"])
        assertEquals("$expectedBaseUrl/api/auth/userinfo", result["userinfo_endpoint"])
    }
}
