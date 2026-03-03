package com.synchtask.friend.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class FriendRequestDTO(
    @field:NotBlank(message = "Friend email is required")
    @field:Email(message = "Friend email should be valid")
    val friendEmail: String
)
