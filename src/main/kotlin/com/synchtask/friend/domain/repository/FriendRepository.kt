package com.synchtask.friend.domain.repository

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FriendRepository : JpaRepository<Friend, Long> {

    fun findByRequesterIdAndFriendId(
        requesterId: Long,
        friendId: Long
    ): Friend?

    fun findAllByRequesterIdOrFriendId(
        requesterId: Long,
        friendId: Long
    ): List<Friend>

    fun findByRequesterIdAndStatus(
        requesterId: Long,
        status: FriendshipStatus
    ): List<Friend>

    fun findByFriendIdAndStatus(
        friendId: Long,
        status: FriendshipStatus
    ): List<Friend>

    @Query(
        """
        select count(f) > 0
        from Friend f
        where f.status = 'ACCEPTED'
          and (
            (f.requester.id = :userId and f.friend.id = :otherUserId)
            or
            (f.requester.id = :otherUserId and f.friend.id = :userId)
          )
        """
    )
    fun existsAcceptedFriendship(
        @Param("userId") userId: Long,
        @Param("otherUserId") otherUserId: Long
    ): Boolean
}
