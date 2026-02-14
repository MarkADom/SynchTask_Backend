package com.synchtask.security.presentation.controller

import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Exposes the public JWT keys in JWKS format.
 *
 * Used by resource servers to validate incoming access tokens.
 */
@RestController
@RequestMapping("/jwks")
class JwksController(
    private val jwtKeyManager: JwtKeyManager
) {
    private val logger =
        LoggerFactory.getLogger(JwksController::class.java)

    @GetMapping
    fun getJwks(): ResponseEntity<Map<String, Any>> {
        return try {
            val jwks = jwtKeyManager.getJwks()
            ResponseEntity.ok(jwks)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve JWKS", ex)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "error" to "JWKS retrieval failed",
                    "details" to (ex.message ?: "Unexpected error")
                )
            )
        }
    }
}
