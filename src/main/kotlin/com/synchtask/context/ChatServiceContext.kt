package com.synchtask.context

import com.fasterxml.jackson.databind.ObjectMapper
import com.synchtask.managers.WebSocketManager
import com.synchtask.chat.domain.repository.ChatMessageRepository
import com.synchtask.chat.domain.repository.ChatRoomRepository
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.services.redis.RedisPublisher
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
