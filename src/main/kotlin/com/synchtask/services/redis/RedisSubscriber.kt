package com.synchtask.services.redis

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.synchtask.dtos.chat.ChatMessageDTO
import com.synchtask.dtos.notification.NotificationDTO
import com.synchtask.managers.WebSocketManager
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class RedisSubscriber(
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val webSocketManager: WebSocketManager
) {

    private val logger = LoggerFactory.getLogger(RedisSubscriber::class.java)

    private fun <T> processMessage(
        message: String,
        type: Class<T>,
        destinationFunction: (T) -> String
    ) {
        try {
            val parsedMessage: T = objectMapper.readValue(message, type)
            val destination = destinationFunction(parsedMessage)

            messagingTemplate.convertAndSend(destination, parsedMessage!!)
            logger.info("Message forwarded to WebSocket [{}]: {}", destination, parsedMessage)

        } catch (ex: JsonProcessingException) {
            logger.error("Invalid JSON format received from Redis: {}", message, ex)
        } catch (ex: MessagingException) {
            logger.error("WebSocket messaging error while sending message: {}", message, ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid argument encountered while processing message: {}", message, ex)
        }
    }

    fun handleMessage(message: String) {
        processMessage(
            message,
            ChatMessageDTO::class.java
        ) { chatMessage: ChatMessageDTO -> "/topic/chat/${chatMessage.chatRoomId}" }
    }

    fun handleNotificationMessage(message: String) {
        processMessage(
            message,
            NotificationDTO::class.java
        ) { notification: NotificationDTO -> "/user/queue/notifications/${notification.recipientEmail}" }
    }
}
