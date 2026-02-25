package com.synchtask.security.presentation.controller

import com.synchtask.security.application.dto.JwkKeyDTO
import com.synchtask.security.application.dto.JwksResponseDTO
import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import com.synchtask.shared.dto.ApiMessageResponseDTO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class JwksControllerTest {
    private lateinit var jwtKeyManager: JwtKeyManager
    private lateinit var controller: JwksController

    @BeforeEach
    fun setup() {
        jwtKeyManager = mockk()
        controller = JwksController(jwtKeyManager)
    }

    @Test
    fun `should return JWKS successfully`() {
        val jwksMock = JwksResponseDTO(keys = listOf(JwkKeyDTO("RSA", "RS256", "sig", "n", "e", "key1")))

        every { jwtKeyManager.getJwks() } returns jwksMock

        val response = controller.getJwks()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(jwksMock, response.body)
        verify(exactly = 1) { jwtKeyManager.getJwks() }
    }

    @Test
    fun `should return 500 error when JWKS retrieval fails`() {
        every { jwtKeyManager.getJwks() } throws RuntimeException("Simulated failure")

        val response = controller.getJwks()

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("JWKS retrieval failed", (response.body as ApiMessageResponseDTO).message)

        verify(exactly = 1) { jwtKeyManager.getJwks() }
    }
}
