package com.synchtask.security.infrastructure.jwt

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt

class CustomJwtAuthenticationConverterTest {
    private val userDetailsService: UserDetailsService = mockk()
    private val converter = CustomJwtAuthenticationConverter(userDetailsService)

    @Test
    fun `should convert JWT with multiple roles`() {
        val jwt = mockJwt("john.doe", listOf("ADMIN", "USER"))
        val userDetails = buildUser("john.doe")

        every { userDetailsService.loadUserByUsername("john.doe") } returns userDetails

        val auth = converter.convert(jwt)

        assertEquals(userDetails, auth.principal)
        assertEquals("fake-token", auth.credentials)
        assertTrue(auth.authorities.map(GrantedAuthority::getAuthority).containsAll(listOf("ROLE_ADMIN", "ROLE_USER")))
    }

    @Test
    fun `should convert JWT with single string role`() {
        val jwt = mockJwt("john.doe", "USER")
        val userDetails = buildUser("john.doe")

        every { userDetailsService.loadUserByUsername("john.doe") } returns userDetails

        val auth = converter.convert(jwt)

        assertEquals(userDetails, auth.principal)
        assertEquals(1, auth.authorities.size)
        assertEquals("ROLE_USER", auth.authorities.first().authority)
    }

    @Test
    fun `should convert JWT with no roles`() {
        val jwt = mockJwt("john.doe", null)
        val userDetails = buildUser("john.doe")

        every { userDetailsService.loadUserByUsername("john.doe") } returns userDetails

        val auth = converter.convert(jwt)

        assertEquals(userDetails, auth.principal)
        assertTrue(auth.authorities.isEmpty())
    }

    @Test
    fun `should throw if sub is missing`() {
        val claims = mapOf("roles" to listOf("USER"))
        val jwt = mockk<Jwt>()
        every { jwt.claims } returns claims

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                converter.convert(jwt)
            }

        assertEquals("JWT does not contain 'sub' claim", exception.message)
    }

    // Helpers

    private fun mockJwt(sub: String?, roles: Any?): Jwt {
        val claims = mutableMapOf<String, Any>()
        if (sub != null) claims["sub"] = sub
        if (roles != null) claims["roles"] = roles

        val jwt = mockk<Jwt>(relaxed = true)
        every { jwt.claims } returns claims
        every { jwt.tokenValue } returns "fake-token"
        return jwt
    }

    private fun buildUser(username: String): UserDetails {
        return User(username, "password", emptyList())
    }
}
