package com.synchtask.websocket.application.handler

import com.synchtask.chat.application.dto.WebSocketMessageDTO
import com.synchtask.chat.application.service.ChatWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketHandler(
    private val chatWebSocketService: ChatWebSocketService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)

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
