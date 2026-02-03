package com.synchtask.controllers

import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import com.synchtask.security.presentation.controller.AuthAdminController
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * **AuthAdminControllerTest**
 *
 * - Unit test for JWT key rotation endpoint.
 * - Uses pure MockK with manual injection.
 */
class AuthAdminControllerTest {

    private lateinit var jwtKeyManager: JwtKeyManager
    private lateinit var authAdminController: AuthAdminController

    @BeforeEach
    fun setup() {
        jwtKeyManager = mockk(relaxed = true)
        authAdminController = AuthAdminController(jwtKeyManager)
    }

    @Test
    fun `should rotate JWT keys and return success message`() {
        // Act
        val response = authAdminController.rotateKeys()

        // Assert
        assertEquals(mapOf("message" to "JWT keys rotated successfully"), response)
        verify(exactly = 1) { jwtKeyManager.rotateKeys() }
    }
}
