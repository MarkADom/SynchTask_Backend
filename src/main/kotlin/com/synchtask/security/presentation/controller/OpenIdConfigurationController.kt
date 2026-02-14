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
        val baseUrl = "${request.scheme}://${request.serverName}:${request.serverPort}/api/auth"
        return mapOf(
            "issuer" to baseUrl,
            "jwks_uri" to "$baseUrl/api/jwks",
            "authorization_endpoint" to "$baseUrl/api/auth/login",
            "token_endpoint" to "$baseUrl/api/auth/token",
            "userinfo_endpoint" to "$baseUrl/api/auth/userinfo"
        )
    }
}
