package com.synchtask.repositories

import com.synchtask.entities.ChatMessage
import com.synchtask.entities.ChatRoom
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * **Chat Message Repository**
 *
 * Handles database operations for chat messages.
 */
@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    /**
     * **Finds all messages for a specific chat room, ordered by timestamp (oldest first).**
     * Uses `@EntityGraph` to avoid Lazy Loading issues with sender details.
     */
    @EntityGraph(attributePaths = ["sender"])
    fun findByChatRoomOrderByTimestampAsc(chatRoom: ChatRoom): List<ChatMessage>
}
