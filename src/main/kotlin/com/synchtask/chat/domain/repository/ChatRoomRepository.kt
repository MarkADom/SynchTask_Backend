package com.synchtask.chat.domain.repository

import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.user.domain.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    @EntityGraph(attributePaths = ["participants"])
    @Query(
        """
        SELECT c FROM ChatRoom c
        WHERE SIZE(c.participants) = :size 
        AND :participant MEMBER OF c.participants
    """
    )
    fun findByExactParticipants(@Param("participant") participant: Set<User>, @Param("size") size: Int): ChatRoom?
}
