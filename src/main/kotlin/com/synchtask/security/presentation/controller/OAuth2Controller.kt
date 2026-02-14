package com.synchtask.security.presentation.controller

import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * OAuth2 user inspection endpoint.
 *
 * Useful for client-side context and debugging OAuth2 sessions.
 */
@RestController
@RequestMapping("/oauth2")
class OAuth2Controller {
    private val logger = LoggerFactory.getLogger(OAuth2Controller::class.java)

    /**
     * Returns information about the currently authenticated OAuth2 user.
     *
     * @param user The OAuth2 authenticated user principal.
     * @return A map containing email, name and roles.
     */
    @GetMapping("/com/synchtask/user")
    fun getAuthenticatedUser(@AuthenticationPrincipal user: OAuth2User): Map<String, Any> {
        val email = user.attributes["email"] ?: "unknown"
        val name = user.attributes["name"] ?: "unknown"
        val roles = user.authorities.map { it.authority }

        logger.info("OAuth2 User Authenticated: email=$email, name=$name")

        return mapOf(
            "email" to email,
            "name" to name,
            "roles" to roles
        )
    }
}
