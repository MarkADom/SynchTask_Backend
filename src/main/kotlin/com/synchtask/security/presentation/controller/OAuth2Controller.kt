package com.synchtask.security.presentation.controller

import com.synchtask.security.application.dto.OidcUserInfoDTO
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

    @GetMapping("/com/synchtask/user")
    fun getAuthenticatedUser(@AuthenticationPrincipal user: OAuth2User): OidcUserInfoDTO {
        logger.info("OAuth2 user authenticated")

        return OidcUserInfoDTO(
            email = user.attributes["email"]?.toString().orEmpty(),
            name = user.attributes["name"]?.toString().orEmpty(),
            roles = user.authorities.map { it.authority }
        )
    }
}
