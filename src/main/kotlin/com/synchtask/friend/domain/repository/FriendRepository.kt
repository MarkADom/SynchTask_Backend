package com.synchtask.friend.domain.repository

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FriendRepository : JpaRepository<Friend, Long> {
    fun findByRequesterIdAndFriendId(requesterId: Long, friendId: Long): Friend?

    fun findAllByRequesterIdOrFriendId(requesterId: Long, friendId: Long): List<Friend>

    fun findByRequesterIdAndStatus(requesterId: Long, status: FriendshipStatus): List<Friend>

    fun findByFriendIdAndStatus(friendId: Long, status: FriendshipStatus): List<Friend>

    @Query(
        """
    select count(f) > 0
    from Friend f
    where f.status = :status
      and (
        (f.requesterId = :userId and f.friendId = :otherUserId)
        or
        (f.requesterId = :otherUserId and f.friendId = :userId)
      )
    """
    )
    fun existsFriendshipBetween(
        @Param("userId") userId: Long,
        @Param("otherUserId") otherUserId: Long,
        @Param("status") status: FriendshipStatus = FriendshipStatus.ACCEPTED,
    ): Boolean
}
