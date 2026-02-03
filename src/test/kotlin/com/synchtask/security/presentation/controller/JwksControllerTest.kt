package com.synchtask.security.presentation.controller

import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        // given
        val jwksMock = mapOf("keys" to listOf(mapOf("kty" to "RSA", "kid" to "key1")))

        every { jwtKeyManager.getJwks() } returns jwksMock

        // when
        val response = controller.getJwks()

        // then
        assertAll(
            { assertEquals(HttpStatus.OK, response.statusCode) },
            { assertEquals(jwksMock, response.body) }
        )
        verify(exactly = 1) { jwtKeyManager.getJwks() }
    }

    @Test
    fun `should return 500 error when JWKS retrieval fails`() {
        // given
        every { jwtKeyManager.getJwks() } throws RuntimeException("Simulated failure")

        // when
        val response = controller.getJwks()

        // then
        assertAll(
            { assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode) },
            { assertTrue(response.body?.containsKey("error") == true) },
            { assertEquals("JWKS retrieval failed", response.body?.get("error")) }
        )
        verify(exactly = 1) { jwtKeyManager.getJwks() }
    }
}
