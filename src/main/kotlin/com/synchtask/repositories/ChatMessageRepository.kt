package com.synchtask.repositories

import com.synchtask.entities.ChatMessage
import com.synchtask.entities.ChatRoom
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = ["sender"])
    fun findByChatRoomOrderByTimestampAsc(chatRoom: ChatRoom): List<ChatMessage>
}
