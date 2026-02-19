package com.synchtask.security.presentation.controller

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
    fun openIdConfig(request: HttpServletRequest): Map<String, Any> {
        val rootUrl = "${request.scheme}://${request.serverName}:${request.serverPort}"
        return mapOf(
            "issuer" to rootUrl,
            "jwks_uri" to "$rootUrl/jwks",
            "authorization_endpoint" to "$rootUrl/oauth2/authorization/google",
            "token_endpoint" to "$rootUrl/auth/refresh",
            "userinfo_endpoint" to "$rootUrl/oauth2/com/synchtask/user"
        )
    }
}
