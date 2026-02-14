package com.synchtask.websocket.application.handler

import com.synchtask.websocket.application.manager.WebSocketManager
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.security.Principal
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class WebSocketReconnectionHandlerTest {
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var scheduler: ScheduledExecutorService
    private lateinit var handler: WebSocketReconnectionHandler

    @BeforeEach
    fun setup() {
        webSocketManager = mockk(relaxed = true)
        messagingTemplate = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)

        handler = spyk(WebSocketReconnectionHandler(webSocketManager, messagingTemplate), recordPrivateCalls = true)

        val schedulerField = handler.javaClass.getDeclaredField("scheduler")
        schedulerField.isAccessible = true
        schedulerField.set(handler, scheduler)
    }

    @Test
    fun `should register session on connect`() {
        val sessionId = "sess-123"
        val userEmail = "user@example.com"

        val accessor = StompHeaderAccessor.create()
        accessor.sessionId = sessionId
        accessor.user = Principal { userEmail }
        accessor.setLeaveMutable(true)

        val message: Message<ByteArray> = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val event = SessionConnectEvent(this, message)

        handler.handleConnect(event)
    }

    @Test
    fun `should schedule reconnection on disconnect`() {
        val sessionId = "sess-456"
        val userEmail = "offline@example.com"

        val accessor = StompHeaderAccessor.create()
        accessor.sessionId = sessionId
        accessor.setLeaveMutable(true)

        val message: Message<ByteArray> = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

        val sessionField = handler.javaClass.getDeclaredField("activeSessions")
        sessionField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val activeSessions = sessionField.get(handler) as MutableMap<String, String>
        activeSessions[sessionId] = userEmail

        every { webSocketManager.isUserOnline(userEmail) } returns false

        val event = SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL)

        handler.handleDisconnect(event)

        verify {
            scheduler.schedule(any(), any(), eq(TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun `should not reconnect if user is already online`() {
        val userEmail = "already@online.com"
        every { webSocketManager.isUserOnline(userEmail) } returns true

        val method = handler.javaClass.getDeclaredMethod("scheduleReconnection", String::class.java, Int::class.java)
        method.isAccessible = true
        method.invoke(handler, userEmail, 1)

        verify(exactly = 0) {
            scheduler.schedule(any(), any(), any())
        }
    }
}
