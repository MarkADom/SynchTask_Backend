package com.synchtask.security.presentation.controller

import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import com.synchtask.shared.dto.ApiMessageResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin endpoint for manual JWT key rotation.
 */
@RestController
@RequestMapping("/auth")
class AuthAdminController(private val jwtKeyManager: JwtKeyManager) {
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/rotate-keys")
    fun rotateKeys(): ResponseEntity<ApiMessageResponseDTO> {
        return try {
            jwtKeyManager.rotateKeys()
            ResponseEntity.ok(ApiMessageResponseDTO("JWT keys rotated successfully"))
        } catch (_: UnsupportedOperationException) {
            ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                ApiMessageResponseDTO("Manual rotation is not available for file-based keys.")
            )
        }
    }
}
