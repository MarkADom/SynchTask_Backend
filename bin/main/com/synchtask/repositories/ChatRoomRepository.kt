package com.synchtask.repositories

import com.synchtask.entities.ChatRoom
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


/**
 * **Chat Room Repository**
 *
 * Provides database operations for `ChatRoom` entities.
 */
@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {

    /**
     * **Finds a chat room by its exact participants.**
     *
     * Uses `@EntityGraph` to evitar Lazy Loading issues.
     *
     * @param participants The list of users in the chat.
     * @return A `ChatRoom` entity if found, otherwise `null`.
     */
    @EntityGraph(attributePaths = ["participants"])
    @Query(
        """
        SELECT c FROM ChatRoom c
        WHERE SIZE(c.participants) = :size 
        AND :participant MEMBER OF c.participants
    """
    )
    fun findByExactParticipants(
        @Param("participant") participant: Set<User>,
        @Param("size") size: Int
    ): ChatRoom?
}
