package com.synchtask.friend.domain.repository

import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import org.springframework.data.jpa.repository.JpaRepository
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
}
