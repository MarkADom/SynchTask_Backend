package com.synchtask.friend.domain.repository

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.user.domain.entity.User
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
