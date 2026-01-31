package com.synchtask.services.friend

import com.synchtask.entities.Friend
import com.synchtask.entities.FriendshipStatus
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.repositories.FriendRepository
import com.synchtask.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * **Friend Service**
 *
 * Handles friend requests, approvals, and removals.
 */
@Service
class FriendService(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(FriendService::class.java)

    /**
     * **Sends a friend request**
     */
    @Transactional
    fun sendFriendRequest(requesterEmail: String, friendEmail: String): Friend {
        val requester = userRepository.findByEmail(requesterEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $requesterEmail") }

        val friend = userRepository.findByEmail(friendEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $friendEmail") }

        require(friendRepository.findByRequesterAndFriend(requester, friend) == null) {
            "Friend request already sent!"
        }

        val newFriendRequest = Friend(requester = requester, friend = friend)
        logger.info("Friend request sent from \${requester.email} to \${friend.email}")
        return friendRepository.save(newFriendRequest)
    }

    /**
     * **Accepts a friend request**
     */
    @Transactional
    fun acceptFriendRequest(friendRequestId: Long, userEmail: String): Friend {
        val friendRequest = friendRepository.findById(friendRequestId)
            .orElseThrow { ResourceNotFoundException("Friend request not found") }

        if (friendRequest.friend.email != userEmail) {
            throw UnauthorizedAccessException("You are not authorized to accept this request")
        }

        friendRequest.status = FriendshipStatus.ACCEPTED
        logger.info("Friend request accepted between \${friendRequest.requester.email} and \${friendRequest.friend.email}")
        return friendRepository.save(friendRequest)
    }

    /**
     * **Rejects or removes a friend**
     */
    @Transactional
    fun removeFriend(friendRequestId: Long, userEmail: String) {
        val friendRequest = friendRepository.findById(friendRequestId)
            .orElseThrow { ResourceNotFoundException("Friend request not found") }

        if (friendRequest.requester.email != userEmail && friendRequest.friend.email != userEmail) {
            throw UnauthorizedAccessException("You are not authorized to remove this friendship")
        }

        friendRepository.delete(friendRequest)
        logger.info("Friendship removed between \${friendRequest.requester.email} and \${friendRequest.friend.email}")
    }

    /**
     * **List all friends of a user**
     */
    fun listFriends(email: String): List<Friend> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("User not found: $email") }

        return friendRepository.findByRequesterAndStatus(user, FriendshipStatus.ACCEPTED) +
                friendRepository.findByFriendAndStatus(user, FriendshipStatus.ACCEPTED)
    }

    /**
     * Retrieves a list of emails of the user's friends.
     */
    fun getUserFriends(email: String): List<String> {
        return friendRepository.findFriendsByRequesterEmailOrFriendEmailAndStatus(
            requesterEmail = email,
            friendEmail = email,
            status = FriendshipStatus.ACCEPTED
        ).map {
            if (it.requester.email == email) it.friend.email else it.requester.email
        }
    }
}
