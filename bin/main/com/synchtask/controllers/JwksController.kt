package com.synchtask.controllers

import com.synchtask.security.JwtKeyManager
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * **JWKS Controller**
 * - Exposes the public RSA key in JWKS format via `/jwks` endpoint.
 * - Used by OAuth2 Resource Server to validate incoming JWTs.
 */
@RestController
@RequestMapping("/jwks")
class JwksController(
    private val jwtKeyManager: JwtKeyManager
) {

    private val logger = LoggerFactory.getLogger(JwksController::class.java)

    /**
     * Returns the current public RSA key set in JWKS format.
     *
     * @return JSON Web Key Set response.
     */
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
