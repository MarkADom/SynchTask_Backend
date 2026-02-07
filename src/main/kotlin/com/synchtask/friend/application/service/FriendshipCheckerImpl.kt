package com.synchtask.friend.application.service

import com.synchtask.friend.application.port.FriendshipChecker
import com.synchtask.friend.domain.repository.FriendRepository
import org.springframework.stereotype.Service

@Service
class FriendshipCheckerImpl(
    private val friendRepository: FriendRepository
) : FriendshipChecker {

    override fun areFriends(userId: Long, otherUserId: Long): Boolean {
        return friendRepository.existsFriendshipBetween(userId, otherUserId)
    }
}
