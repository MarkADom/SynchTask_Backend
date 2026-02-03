package com.synchtask.chat.application.context

import com.fasterxml.jackson.databind.ObjectMapper
import com.synchtask.chat.domain.repository.ChatMessageRepository
import com.synchtask.chat.domain.repository.ChatRoomRepository
import com.synchtask.websocket.application.manager.WebSocketManager
import com.synchtask.redis.application.service.RedisPublisher
import com.synchtask.user.domain.repository.UserRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * Dependency container for chat-related services.
 *
 * Keeps WebSocket and persistence wiring explicit
 * without overloading service constructors.
 */
@Component
data class ChatServiceContext(
    val messagingTemplate: SimpMessagingTemplate,
    val chatMessageRepository: ChatMessageRepository,
    val chatRoomRepository: ChatRoomRepository,
    val userRepository: UserRepository,
    val webSocketManager: WebSocketManager,
    val redisPublisher: RedisPublisher,
    val objectMapper: ObjectMapper,
)
