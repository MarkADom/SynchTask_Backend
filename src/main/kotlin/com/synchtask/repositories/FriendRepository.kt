package com.synchtask.repositories

import com.synchtask.entities.Friend
import com.synchtask.entities.FriendshipStatus
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FriendRepository : JpaRepository<Friend, Long> {

    @EntityGraph(attributePaths = ["requester", "friend"])
    fun findByRequesterAndFriend(requester: User, friend: User): Friend?

    @EntityGraph(attributePaths = ["requester", "friend"])
    fun findByFriendAndStatus(friend: User, status: FriendshipStatus): List<Friend>

    @EntityGraph(attributePaths = ["requester", "friend"])
    fun findByRequesterAndStatus(requester: User, status: FriendshipStatus): List<Friend>

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
        status: FriendshipStatus,
    ): List<Friend>

    @EntityGraph(attributePaths = ["requester", "friend"])
    @Query(
        """
    SELECT f FROM Friend f
    WHERE f.requester = :user OR f.friend = :user
    """
    )
    fun findAllByUserInvolved(user: User): List<Friend>

}
