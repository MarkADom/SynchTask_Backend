package com.synchtask.friend.application.mapper

import com.synchtask.friend.application.dto.FriendResponseDTO
import com.synchtask.friend.domain.entity.Friend
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository

class FriendMapper(
    private val userRepository: UserRepository
) {
    fun toResponse(friend: Friend, currentUserId: Long, usersById: Map<Long, User>): FriendResponseDTO {
        val requester = usersById[friend.requesterId]
            ?: userRepository.findById(friend.requesterId)
                .orElseThrow { ResourceNotFoundException("User not found: ${friend.requesterId}") }

        val friendUser = usersById[friend.friendId]
            ?: userRepository.findById(friend.friendId)
                .orElseThrow { ResourceNotFoundException("User not found: ${friend.friendId}") }

        val isIncoming = friend.friendId == currentUserId

        return FriendResponseDTO(
            id = friend.id!!,
            friendEmail = if (isIncoming) requester.email else friendUser.email,
            requesterEmail = requester.email,
            status = friend.status.name,
            isIncoming = isIncoming
        )
    }
}
