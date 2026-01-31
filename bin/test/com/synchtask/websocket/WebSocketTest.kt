package com.synchtask.websocket

import com.synchtask.BaseTest
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.repositories.UserRepository
import com.synchtask.security.JwtTokenProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * Integration test for WebSocket connection with valid JWT.
 * It creates a test user, generates a JWT, and attempts to connect to `/ws-test`.
 */
class WebSocketTest @Autowired constructor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) : BaseTest() {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `should connect to WebSocket server`() = runBlocking {
        val email = "testuser@synchtask.com"
        val passwordHash = "irrelevant"

        // Clean up any existing user with this email
        userRepository.findByEmail(email).ifPresent { userRepository.delete(it) }

        // Create new test user
        val testUser = User(
            email = email,
            name = "Test User",
            passwordHash = passwordHash,
            isActive = true,
            role = UserRole.USER
        )
        userRepository.save(testUser)

        val userDetails = org.springframework.security.core.userdetails.User
            .withUsername(email)
            .password(passwordHash)
            .roles("USER")
            .build()

        val jwt = jwtTokenProvider.generateToken(userDetails)
        val connectUrl = "ws://localhost:$port/ws-test?token=$jwt"

        println("Connecting to: $connectUrl")

        val stompClient = WebSocketStompClient(StandardWebSocketClient()).apply {
            messageConverter = MappingJackson2MessageConverter()
        }

        val future = CompletableFuture<StompSession>()
        val handler = object : StompSessionHandlerAdapter() {
            override fun afterConnected(session: StompSession, connectedHeaders: org.springframework.messaging.simp.stomp.StompHeaders) {
                println("Connected: session=${session.sessionId}")
                future.complete(session)
            }

            override fun handleTransportError(session: StompSession, exception: Throwable) {
                println("Error: ${exception.message}")
                future.completeExceptionally(exception)
            }
        }

        stompClient.connectAsync(connectUrl, handler)

        val session = future.get(5, TimeUnit.SECONDS)
        assertTrue(session.isConnected, "WebSocket should be connected")
    }
}
