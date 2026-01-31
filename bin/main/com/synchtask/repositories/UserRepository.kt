package com.synchtask.repositories

import com.synchtask.entities.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * **User Repository**
 *
 * Interface responsible for database operations on `User` entities.
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     *
     * Eagerly loads `chatRooms`, `receivedFriendRequests`, and `sentFriendRequests`.
     *
     * @param email The email address to search for.
     * @return An optional containing the user, if found.
     */
    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests", "sentFriendRequests"])
    fun findByEmail(email: String): Optional<User>

    /**
     * Finds users by a list of email addresses.
     *
     * Eagerly loads the same relationships as `findByEmail`.
     *
     * @param emails List of emails to match.
     * @return A list of matching users.
     */
    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests", "sentFriendRequests"])
    fun findByEmailIn(emails: List<String>): List<User>

    /**
     * Finds users who were active after a given timestamp.
     *
     * Eagerly loads chat data and friend requests.
     *
     * @param since Timestamp indicating the activity threshold.
     * @return A list of active users since the given time.
     */
    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests"])
    @Query("SELECT u FROM User u WHERE u.lastActivity > :since")
    fun findAllByLastActivityAfter(since: LocalDateTime): List<User>

    /**
     * Finds users currently marked as online.
     *
     * @return A list of online users.
     */
    @EntityGraph(attributePaths = ["chatRooms", "receivedFriendRequests"])
    fun findAllByIsOnlineTrue(): List<User>

    @Query("""
    SELECT u FROM User u
    WHERE (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
    AND (:onlineOnly IS NULL OR u.isOnline = :onlineOnly)
""")
    fun findUsersByFilters(
        @Param("name") name: String?,
        @Param("onlineOnly") onlineOnly: Boolean?,
        pageable: Pageable
    ): Page<User>

}
