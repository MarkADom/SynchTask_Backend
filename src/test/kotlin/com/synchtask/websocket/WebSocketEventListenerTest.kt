package com.synchtask.websocket

import com.synchtask.websocket.application.handler.WebSocketReconnectionHandler
import com.synchtask.user.application.service.UserService
import com.synchtask.websocket.application.event.WebSocketEventListener
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.security.Principal

class WebSocketEventListenerTest {

    private lateinit var userService: UserService
    private lateinit var reconnectionHandler: WebSocketReconnectionHandler
    private lateinit var listener: WebSocketEventListener

    @BeforeEach
    fun setup() {
        userService = mockk(relaxed = true)
        reconnectionHandler = mockk(relaxed = true)
        listener = WebSocketEventListener(userService, reconnectionHandler)
    }

    @Test
    fun `should mark user as online on WebSocket connect`() {
        val principal = Principal { "user@example.com" }

        val message = MessageBuilder.withPayload(ByteArray(0))
            .setHeader("simpUser", principal)
            .build()

        val event = SessionConnectedEvent(this, message)

        listener.handleWebSocketConnectListener(event)

        Thread.sleep(100)

        verify {
            userService.setUserOnlineStatus("user@example.com", true)
        }
    }

    @Test
    fun `should mark user as offline and handle reconnect if needed`() {
        val principal = Principal { "user@example.com" }

        val message = MessageBuilder.withPayload(ByteArray(0))
            .setHeader("simpUser", principal)
            .build()

        val event = SessionDisconnectEvent(this, message, "sess-123", CloseStatus.NORMAL)

        every { reconnectionHandler.shouldAttemptReconnection("user@example.com") } returns true

        listener.handleWebSocketDisconnectListener(event)

        Thread.sleep(100)

        verify {
            userService.setUserOnlineStatus("user@example.com", false)
            reconnectionHandler.handleDisconnect(event)
        }
    }

    @Test
    fun `should not trigger reconnect if shouldAttemptReconnection returns false`() {
        val principal = Principal { "user@example.com" }

        val message = MessageBuilder.withPayload(ByteArray(0))
            .setHeader("simpUser", principal)
            .build()

        val event = SessionDisconnectEvent(this, message, "sess-123", CloseStatus.NORMAL)

        every { reconnectionHandler.shouldAttemptReconnection("user@example.com") } returns false

        listener.handleWebSocketDisconnectListener(event)

        Thread.sleep(100)

        verify(exactly = 0) {
            reconnectionHandler.handleDisconnect(event)
        }

        verify {
            userService.setUserOnlineStatus("user@example.com", false)
        }
    }

    @Test
    fun `should ignore events with null user`() {
        val message = MessageBuilder.withPayload(ByteArray(0)).build()

        val connectEvent = SessionConnectedEvent(this, message)
        val disconnectEvent = SessionDisconnectEvent(this, message, "sess-1", CloseStatus.NORMAL)

        listener.handleWebSocketConnectListener(connectEvent)
        listener.handleWebSocketDisconnectListener(disconnectEvent)

        Thread.sleep(100)

        verify(exactly = 0) {
            userService.setUserOnlineStatus(any(), any())
            reconnectionHandler.handleDisconnect(any())
        }
    }
}
