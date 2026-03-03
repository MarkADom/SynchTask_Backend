package com.synchtask.security.presentation.controller

import com.synchtask.security.application.dto.OpenIdConfigurationDTO
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * OpenID Connect discovery endpoint.
 *
 * Exposes the minimal metadata needed for JWT validation.
 */
@RestController
@RequestMapping("/auth/.well-known")
class OpenIdConfigurationController {
    @GetMapping("/openid-configuration")
    fun openIdConfig(request: HttpServletRequest): OpenIdConfigurationDTO {
        val rootUrl = "${request.scheme}://${request.serverName}:${request.serverPort}"
        return OpenIdConfigurationDTO(
            issuer = rootUrl,
            jwksUri = "$rootUrl/jwks",
            authorizationEndpoint = "$rootUrl/oauth2/authorization/google",
            tokenEndpoint = "$rootUrl/auth/refresh",
            userinfoEndpoint = "$rootUrl/oauth2/com/synchtask/user"
        )
    }
}
