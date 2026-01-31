package com.synchtask.dtos.friend

import com.synchtask.entities.Friend

data class FriendResponseDTO(
    val id: Long,
    val friendEmail: String,
    val requesterEmail: String,
    val status: String,
    val isIncoming: Boolean
) {
    companion object {
        fun fromEntityForUser(friend: Friend, currentUserEmail: String): FriendResponseDTO {
            val isIncoming = friend.friend.email == currentUserEmail

            return FriendResponseDTO(
                id = friend.id ?: 0,
                friendEmail = if (isIncoming) friend.requester.email else friend.friend.email,
                requesterEmail = friend.requester.email,
                status = friend.status.name,
                isIncoming = isIncoming
            )
        }
    }
}
