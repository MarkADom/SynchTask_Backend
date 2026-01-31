package com.synchtask.context

import com.fasterxml.jackson.databind.ObjectMapper
import com.synchtask.managers.WebSocketManager
import com.synchtask.repositories.ChatMessageRepository
import com.synchtask.repositories.ChatRoomRepository
import com.synchtask.repositories.UserRepository
import com.synchtask.services.redis.RedisPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * **Chat Service Context**
 *
 * - Provides dependencies for `ChatWebSocketService`.
 * - Uses `@Component` to allow Spring to inject dependencies.
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
