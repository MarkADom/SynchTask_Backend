package com.synchtask.services.chat

import com.synchtask.entities.ChatMessage
import com.synchtask.repositories.ChatMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class ChatLoggingService(
    private val chatMessageRepository: ChatMessageRepository,
) {
    private val logger = LoggerFactory.getLogger(ChatLoggingService::class.java)

    @Transactional
    fun logMessage(chatMessage: ChatMessage) {
        try {
            chatMessageRepository.save(chatMessage)
            logger.info(
                "Chat message logged from user: ${chatMessage.sender.email} " +
                        "in chat room ${chatMessage.chatRoom.id}"
            )
        } catch (ex: Exception) {
            logger.error("Failed to log chat message for user: ${chatMessage.sender.email}", ex)
            throw IllegalStateException("Unable to persist chat message.", ex)
        }
    }

    fun countMessages(): Long {
        return chatMessageRepository.count()
    }

    @Transactional
    fun clearAllLogs(): Long {
        val count = chatMessageRepository.count()
        chatMessageRepository.deleteAll()
        logger.warn("Cleared all chat logs. Total messages deleted: $count")
        return count
    }
}
