package com.synchtask.security.presentation.controller

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class OAuth2ControllerTest {
    private val controller = OAuth2Controller()

    @Test
    fun `should return OAuth2 user attributes`() {
        val attributes =
            mapOf(
                "email" to "oauth@example.com",
                "name" to "OAuth User"
            )

        val authorities =
            listOf(
                GrantedAuthority { "ROLE_USER" },
                GrantedAuthority { "ROLE_COLLABORATOR" }
            )

        val oauth2User = mockk<OAuth2User>()
        every { oauth2User.attributes } returns attributes
        every { oauth2User.authorities } returns authorities

        val result = controller.getAuthenticatedUser(oauth2User)

        assertEquals("oauth@example.com", result.email)
        assertEquals("OAuth User", result.name)
        assertEquals(listOf("ROLE_USER", "ROLE_COLLABORATOR"), result.roles)
    }

    @Test
    fun `should fallback to empty values when email or name is missing`() {
        val oauth2User = mockk<OAuth2User>()
        every { oauth2User.attributes } returns emptyMap()
        every { oauth2User.authorities } returns emptyList()

        val result = controller.getAuthenticatedUser(oauth2User)

        assertEquals("", result.email)
        assertEquals("", result.name)
        assertEquals(emptyList<String>(), result.roles)
    }
}
