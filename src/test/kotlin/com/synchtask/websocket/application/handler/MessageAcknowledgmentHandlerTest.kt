package com.synchtask.websocket.application.handler

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

class MessageAcknowledgmentHandlerTest {
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var handler: MessageAcknowledgmentHandler

    @BeforeEach
    fun setup() {
        messagingTemplate = mockk(relaxed = true)
        handler = MessageAcknowledgmentHandler(messagingTemplate)
    }

    @Test
    fun `should acknowledge message when message-id is present`() {
        val messageId = "123-abc"
        val message = buildStompMessageWithHeader("message-id", messageId)

        handler.handleMessageAcknowledgment(message)

        verify {
            messagingTemplate.convertAndSend(
                eq("/queue/acknowledgment/$messageId"),
                eq("ACK: $messageId")
            )
        }
    }

    @Test
    fun `should not acknowledge message when message-id is missing`() {
        val message = buildStompMessageWithHeader("irrelevant-header", "value")

        handler.handleMessageAcknowledgment(message)

        confirmVerified(messagingTemplate)
    }

    private fun buildStompMessageWithHeader(headerName: String, headerValue: String): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create()
        accessor.setNativeHeader(headerName, headerValue)
        accessor.sessionId = "test-session"
        accessor.destination = "/some-destination"
        accessor.setLeaveMutable(true)

        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}
