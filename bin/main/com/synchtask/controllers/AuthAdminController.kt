package com.synchtask.controllers

import com.synchtask.security.JwtKeyManager
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * **Auth Controller**
 * Endpoint for manual rotation of JWT keys.
 */
@RestController
@RequestMapping("/auth")
class AuthAdminController(private val jwtKeyManager: JwtKeyManager) {

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/rotate-keys")
    fun rotateKeys(): Map<String, String> {
        jwtKeyManager.rotateKeys()
        return mapOf("message" to "JWT keys rotated successfully")
    }
}
