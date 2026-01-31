package com.synchtask.websocket

import com.synchtask.dtos.chat.WebSocketMessageDTO
import com.synchtask.services.chat.ChatWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

/**
 * WebSocket Chat Controller
 *
 * Handles encrypted chat messages sent via STOMP over WebSocket.
 * Validates message content and relays it to the appropriate topic.
 */
@Controller
class ChatWebSocketHandler(
    private val chatWebSocketService: ChatWebSocketService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)

    /**
     * Handles encrypted messages sent to /app/chat/send
     *
     * - Validates content.
     * - Stores or processes the message via the service layer.
     * - Broadcasts to /topic/chat/{chatRoomId}.
     *
     * @param message DTO with encrypted chat payload.
     */
    @MessageMapping("/chat/send")
    fun handleMessage(@Payload message: WebSocketMessageDTO) {
        if (message.encryptedMessage.isBlank()) {
            logger.warn("Rejected empty encrypted message from: ${message.senderEmail}")
            return
        }

        logger.debug("Received chat message for room ${message.chatRoomId} from ${message.senderEmail}")

        // Process and persist message if needed
        chatWebSocketService.sendMessage(
            chatRoomId = message.chatRoomId,
            senderEmail = message.senderEmail,
            encryptedMessage = message.encryptedMessage
        )

        // Broadcast to all clients subscribed to the room
        messagingTemplate.convertAndSend("/topic/chat/${message.chatRoomId}", message)
    }
}
