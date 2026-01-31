package com.synchtask.handlers

import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component

/**
 * **Message Acknowledgment Handler**
 *
 * - Ensures messages are acknowledged before being marked as received.
 * - Prevents message loss and duplicate messages.
 */
@Component
class MessageAcknowledgmentHandler(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(MessageAcknowledgmentHandler::class.java)

    /**
     * **Handles message acknowledgment from STOMP messages**
     *
     * @param message The incoming STOMP message.
     */
    fun handleMessageAcknowledgment(message: Message<*>) {
        val accessor = StompHeaderAccessor.wrap(message)
        val messageId = accessor.getFirstNativeHeader("message-id")

        if (!messageId.isNullOrEmpty()) {
            logger.info("✅ Message acknowledged: $messageId")

            // Send confirmation back to sender
            val destination = "/queue/acknowledgment/$messageId"
            messagingTemplate.convertAndSend(destination, "ACK: $messageId")
        } else {
            logger.warn("⚠ Message acknowledgment failed: No message ID found in headers")
        }
    }
}
