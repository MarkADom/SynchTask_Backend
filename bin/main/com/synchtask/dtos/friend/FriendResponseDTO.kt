package com.synchtask.dtos.friend

import com.synchtask.entities.Friend

/**
 * **DTO for returning a friend list**
 */
data class FriendResponseDTO(
    val id: Long,
    val friendEmail: String,
    val status: String
) {
    companion object {
        fun fromEntity(friend: Friend): FriendResponseDTO {
            return FriendResponseDTO(
                id = friend.id!!,
                friendEmail = friend.friend.email,
                status = friend.status.name
            )
        }
    }
}
