package com.synchtask.friend.application.service

import com.synchtask.friend.application.dto.FriendResponseDTO
import com.synchtask.friend.application.mapper.FriendMapper
import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.friend.domain.exception.FriendRequestAlreadySentException
import com.synchtask.friend.domain.repository.FriendRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FriendService(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
) {

    private val logger = LoggerFactory.getLogger(FriendService::class.java)
    private val mapper = FriendMapper(userRepository)

    @Transactional
    fun sendFriendRequest(requesterEmail: String, friendEmail: String): Friend {
        val requester = userRepository.findByEmail(requesterEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $requesterEmail") }

        val friend = userRepository.findByEmail(friendEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $friendEmail") }

        val existing = friendRepository.findByRequesterIdAndFriendId(requester.id!!, friend.id!!)
            ?: friendRepository.findByRequesterIdAndFriendId(friend.id!!, requester.id!!)

        if (existing != null) {
            throw FriendRequestAlreadySentException("Friend request already exists.")
        }

        val saved = friendRepository.save(
            Friend(
                requesterId = requester.id!!,
                friendId = friend.id!!
            )
        )

        notificationService.sendNotification(
            userEmail = friend.email,
            message = "You have a new friend request from ${requester.email}",
            type = NotificationType.FRIEND_REQUEST
        )

        logger.info("Friend request sent from ${requester.email} to ${friend.email}")
        return saved
    }

    @Transactional
    fun acceptFriendRequest(friendRequestId: Long, userEmail: String): Friend {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val friendRequest = friendRepository.findById(friendRequestId)
            .orElseThrow { ResourceNotFoundException("Friend request not found") }

        if (friendRequest.friendId != user.id) {
            throw UnauthorizedAccessException("You are not authorized to accept this request")
        }

        friendRequest.status = FriendshipStatus.ACCEPTED
        val updated = friendRepository.save(friendRequest)

        notificationService.sendNotification(
            userEmail = userEmail,
            message = "Your friend request was accepted.",
            type = NotificationType.PERSONAL
        )

        return updated
    }

    fun listFriends(email: String): List<FriendResponseDTO> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("User not found: $email") }

        return friendRepository
            .findAllByRequesterIdOrFriendId(user.id!!, user.id!!)
            .map { mapper.toResponse(it, user.id!!) }
    }

    @Transactional
    fun removeFriend(friendRequestId: Long, userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val friendRequest = friendRepository.findById(friendRequestId)
            .orElseThrow { ResourceNotFoundException("Friend request not found") }

        if (friendRequest.requesterId != user.id && friendRequest.friendId != user.id) {
            throw UnauthorizedAccessException("Not authorized to remove this friendship")
        }

        friendRepository.delete(friendRequest)
    }
}
