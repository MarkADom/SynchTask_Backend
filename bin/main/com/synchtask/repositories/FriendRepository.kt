package com.synchtask.repositories

import com.synchtask.entities.Friend
import com.synchtask.entities.FriendshipStatus
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * **Friend Repository**
 *
 * Handles database operations for friendships.
 */
@Repository
interface FriendRepository : JpaRepository<Friend, Long> {

    /**
     * Finds a friendship relation between two users.
     */
    @EntityGraph(attributePaths = ["requester", "friend"])
    fun findByRequesterAndFriend(requester: User, friend: User): Friend?

    /**
     * Finds all friends of a specific user where the friendship status is accepted.
     */
    @EntityGraph(attributePaths = ["requester", "friend"])
    fun findByFriendAndStatus(friend: User, status: FriendshipStatus): List<Friend>

    /**
     * Finds all users to whom the requester has sent friend requests that are accepted.
     */
    @EntityGraph(attributePaths = ["requester", "friend"])
    fun findByRequesterAndStatus(requester: User, status: FriendshipStatus): List<Friend>

    /**
     * **Finds a user's friends based on email**
     *
     * - Look for friends where the user is involved as a “requestster” or “friend”.
     * - Uses “Query” to optimize the search, avoiding multiple queries.
     */
    @Query(
        """
        SELECT f FROM Friend f 
        WHERE (f.requester.email = :requesterEmail OR f.friend.email = :friendEmail)
        AND f.status = :status
    """
    )
    fun findFriendsByRequesterEmailOrFriendEmailAndStatus(
        requesterEmail: String,
        friendEmail: String,
        status: FriendshipStatus
    ): List<Friend>
}
