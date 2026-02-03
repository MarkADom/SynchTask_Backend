package com.synchtask.websocket

import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import com.synchtask.websocket.infrastructure.CustomHandshakeInterceptor
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.socket.WebSocketHandler
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomHandshakeInterceptorTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var interceptor: CustomHandshakeInterceptor
    private lateinit var request: ServerHttpRequest
    private lateinit var response: ServerHttpResponse
    private lateinit var wsHandler: WebSocketHandler
    private lateinit var attributes: MutableMap<String, Any>

    @BeforeEach
    fun setup() {
        jwtTokenProvider = mockk()
        interceptor = CustomHandshakeInterceptor(jwtTokenProvider)
        response = mockk()
        wsHandler = mockk()
        attributes = mutableMapOf()
    }

    @Test
    fun `should prefer Authorization header over query token`() {
        val headerToken = "header.jwt.token"
        val queryToken = "query.jwt.token"

        val userDetails = mockk<UserDetails> {
            every { username } returns "headeruser@example.com"
        }

        request = mockk {
            every { headers } returns HttpHeaders().apply {
                set("Authorization", "Bearer $headerToken")
            }
            every { uri } returns java.net.URI("ws://localhost:8080/ws?token=$queryToken")
        }

        every { jwtTokenProvider.validateAndExtractUser(headerToken) } returns userDetails
        every { jwtTokenProvider.validateAndExtractUser(queryToken) } returns null

        val result = interceptor.beforeHandshake(request, response, wsHandler, attributes)

        assertTrue(result)
        assert(attributes["username"] == "headeruser@example.com")

        verify(exactly = 1) {
            jwtTokenProvider.validateAndExtractUser(headerToken)
        }
        verify(exactly = 0) {
            jwtTokenProvider.validateAndExtractUser(queryToken)
        }
    }


    @Test
    fun `should accept connection with valid Authorization header token`() {
        val token = "valid.jwt.token"
        val userDetails = mockk<UserDetails> {
            every { username } returns "user@example.com"
        }

        request = mockk {
            every { headers } returns HttpHeaders().apply { set("Authorization", "Bearer $token") }
            every { uri } returns java.net.URI("ws://localhost:8080/ws")
        }

        every { jwtTokenProvider.validateAndExtractUser(token) } returns userDetails

        val result = interceptor.beforeHandshake(request, response, wsHandler, attributes)

        assertTrue(result)
        assert(attributes["username"] == "user@example.com")
    }

    @Test
    fun `should accept connection with valid token in query parameter`() {
        val token = "query.jwt.token"
        val userDetails = mockk<UserDetails> {
            every { username } returns "queryuser@example.com"
        }

        request = mockk {
            every { headers } returns HttpHeaders()
            every { uri } returns java.net.URI("ws://localhost:8080/ws?token=$token")
        }

        every { jwtTokenProvider.validateAndExtractUser(token) } returns userDetails

        val result = interceptor.beforeHandshake(request, response, wsHandler, attributes)

        assertTrue(result)
        assert(attributes["username"] == "queryuser@example.com")
    }

    @Test
    fun `should reject connection when no token is provided`() {
        request = mockk {
            every { headers } returns HttpHeaders()
            every { uri } returns java.net.URI("ws://localhost:8080/ws")
        }

        val result = interceptor.beforeHandshake(request, response, wsHandler, attributes)

        assertFalse(result)
    }

    @Test
    fun `should reject connection when token is invalid`() {
        val token = "invalid.token"

        request = mockk {
            every { headers } returns HttpHeaders().apply { set("Authorization", "Bearer $token") }
            every { uri } returns java.net.URI("ws://localhost:8080/ws")
        }

        every { jwtTokenProvider.validateAndExtractUser(token) } returns null

        val result = interceptor.beforeHandshake(request, response, wsHandler, attributes)

        assertFalse(result)
    }
}
