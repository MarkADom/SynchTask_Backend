package com.synchtask.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt

/**
 * **Custom JWT Authentication Converter**
 *
 * - Extracts user details from JWT claims.
 * - Converts `JwtAuthenticationToken` to `UserDetails`.
 * - Assigns roles based on the `roles` claim in the token.
 */
class CustomJwtAuthenticationConverter(
    private val userDetailsService: UserDetailsService
) : Converter<Jwt, UsernamePasswordAuthenticationToken> {

    override fun convert(jwt: Jwt): UsernamePasswordAuthenticationToken {
        val username = jwt.claims["sub"] as String?
            ?: throw IllegalArgumentException("JWT does not contain 'sub' claim")

        val userDetails = userDetailsService.loadUserByUsername(username)

        // Extract roles from the JWT (assuming the claim is "roles")
        val roles = extractRoles(jwt.claims["roles"])
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }

        return UsernamePasswordAuthenticationToken(userDetails, jwt.tokenValue, authorities)
    }

    /**
     * **Extracts roles from JWT claims safely**
     * - Supports different data types (String, List<String>, List<Any>).
     */
    private fun extractRoles(rolesClaim: Any?): List<String> {
        return when (rolesClaim) {
            is String -> listOf(rolesClaim) // Single role as String
            is Collection<*> -> rolesClaim.mapNotNull { it?.toString() } // List of roles
            else -> emptyList() // If claim is null or unexpected type, return empty list
        }
    }
}
