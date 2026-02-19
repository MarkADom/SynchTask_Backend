package com.synchtask.security.presentation.controller

import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus


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

        val response = authAdminController.rotateKeys()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(mapOf("message" to "JWT keys rotated successfully"), response.body)
        verify(exactly = 1) { jwtKeyManager.rotateKeys() }
    }

    @Test
    fun `should return not implemented when key rotation is unsupported`() {
        every { jwtKeyManager.rotateKeys() } throws UnsupportedOperationException("not supported")

        val response = authAdminController.rotateKeys()

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.statusCode)
        assertEquals(
            mapOf("message" to "Manual rotation is not available for file-based keys."),
            response.body
        )

        verify(exactly = 1) { jwtKeyManager.rotateKeys() }
    }
}
