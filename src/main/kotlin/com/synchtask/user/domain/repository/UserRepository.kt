package com.synchtask.user.domain.repository

import com.synchtask.user.domain.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {

    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests", "sentFriendRequests"])
    fun findByEmail(email: String): Optional<User>

    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests", "sentFriendRequests"])
    fun findByEmailIn(emails: List<String>): List<User>

    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests"])
    @Query("SELECT u FROM User u WHERE u.lastActivity > :since")
    fun findAllByLastActivityAfter(since: LocalDateTime): List<User>

    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests"])
    fun findAllByIsOnlineTrue(): List<User>

    @Query(
        """
    SELECT u FROM User u
    WHERE (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
    AND (:onlineOnly IS NULL OR u.isOnline = :onlineOnly)
    """
    )
    fun findUsersByFilters(
        @Param("name") name: String?,
        @Param("onlineOnly") onlineOnly: Boolean?,
        pageable: Pageable,
    ): Page<User>

}
