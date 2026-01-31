package com.synchtask.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Converts a JWT into a Spring Security authentication token.
 *
 * Uses the `sub` claim as username and an optional `roles` claim
 * to build granted authorities.
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

    private fun extractRoles(rolesClaim: Any?): List<String> {
        return when (rolesClaim) {
            is String -> listOf(rolesClaim)
            is Collection<*> -> rolesClaim.mapNotNull { it?.toString() }
            else -> emptyList()
        }
    }
}
