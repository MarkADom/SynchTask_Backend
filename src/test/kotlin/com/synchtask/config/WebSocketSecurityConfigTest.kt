package com.synchtask.config

import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment

class WebSocketSecurityConfigTest {
    @Test
    fun `should build websocket authorization manager`() {
        val jwtTokenProvider = mockk<JwtTokenProvider>(relaxed = true)
        val environment = mockk<Environment>(relaxed = true)
        val corsProperties = CorsProperties().apply { origins = listOf("http://localhost:3000") }

        val config = WebSocketSecurityConfig(jwtTokenProvider, environment, corsProperties)

        val manager = config.webSocketAuthorizationManager()

        assertNotNull(manager)
    }
}
