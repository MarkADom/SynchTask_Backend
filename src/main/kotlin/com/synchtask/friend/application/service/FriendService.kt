package com.synchtask.friend.application.service

import com.synchtask.exception.FriendRequestAlreadySentException
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.friend.domain.entity.Friend
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.friend.domain.repository.FriendRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
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

    /**
     * Sends a friend request.
     */
    @Transactional
    fun sendFriendRequest(requesterEmail: String, friendEmail: String): Friend {
        val requester = userRepository.findByEmail(requesterEmail)
            .orElseThrow {
                ResourceNotFoundException(
                    "User not found: $requesterEmail"
                )
            }

        val friend = userRepository.findByEmail(friendEmail)
            .orElseThrow {
                ResourceNotFoundException(
                    "User not found: $friendEmail"
                )
            }

        // Prevent duplicates both directions
        val existingRelation = friendRepository.findByRequesterAndFriend(requester, friend)
            ?: friendRepository.findByRequesterAndFriend(
                friend,
                requester
            )

        if (existingRelation != null) {
            throw FriendRequestAlreadySentException(
                "Friend request already exists!"
            )
        }

        val newFriendRequest = Friend(requester = requester, friend = friend)
        val savedRequest = friendRepository.save(
            newFriendRequest
        )

        logger.info("Friend request sent from ${requester.email} to ${friend.email}")

        // Notify recipient
        notificationService.sendNotification(
            userEmail = friend.email,
            message = "You have a new friend request from ${requester.email}",
            type = NotificationType.FRIEND_REQUEST
        )

        return savedRequest
    }

    /**
     * Accepts a pending friend request.
     */
    @Transactional
    fun acceptFriendRequest(friendRequestId: Long, userEmail: String): Friend {
        val friendRequest = friendRepository.findById(friendRequestId)
            .orElseThrow {
                ResourceNotFoundException(
                    "Friend request not found"
                )
            }

        if (friendRequest.friend.email != userEmail) {
            throw UnauthorizedAccessException(
                "You are not authorized to accept this request"
            )
        }

        friendRequest.status = FriendshipStatus.ACCEPTED
        val updated = friendRepository.save(friendRequest)

        // Notify requester
        notificationService.sendNotification(
            userEmail = friendRequest.requester.email,
            message = "${friendRequest.friend.email} accepted your friend request!",
            type = NotificationType.PERSONAL
        )

        return updated
    }

    /**
     * Removes friendship or pending request.
     */
    @Transactional
    fun removeFriend(friendRequestId: Long, userEmail: String) {
        val friendRequest = friendRepository.findById(friendRequestId)
            .orElseThrow {
                ResourceNotFoundException(
                    "Friend request not found"
                )
            }

        if (friendRequest.requester.email != userEmail && friendRequest.friend.email != userEmail) {
            throw UnauthorizedAccessException(
                "Not authorized to remove this friendship"
            )
        }

        friendRepository.delete(
            friendRequest
        )
        logger.info("Friendship removed between ${friendRequest.requester.email} and ${friendRequest.friend.email}")
    }

    /**
     * Lists all friend relationships (accepted + pending).
     */

    fun listFriends(email: String): List<Friend> {
        val user = userRepository.findByEmail(email)
            .orElseThrow {
                ResourceNotFoundException(
                    "User not found: $email"
                )
            }

        return friendRepository.findAllByUserInvolved(user)
    }

    /**
     * Returns only accepted friends as User entities.
     */
    fun listFriendUsers(email: String): List<User> {
        val user = userRepository.findByEmail(email)
            .orElseThrow {
                ResourceNotFoundException(
                    "User not found: $email"
                )
            }

        val accepted = friendRepository.findByRequesterAndStatus(
            user,
            FriendshipStatus.ACCEPTED
        ) +
                friendRepository.findByFriendAndStatus(
                    user,
                    FriendshipStatus.ACCEPTED
                )

        return accepted.map { if (it.requester.email == email) it.friend else it.requester }
    }
}
