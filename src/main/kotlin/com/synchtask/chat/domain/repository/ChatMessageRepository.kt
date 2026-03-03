package com.synchtask.chat.domain.repository

import com.synchtask.chat.domain.entity.ChatMessage
import com.synchtask.chat.domain.entity.ChatRoom
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths = ["sender"])
    fun findByChatRoomOrderByTimestampAsc(chatRoom: ChatRoom): List<ChatMessage>
}
