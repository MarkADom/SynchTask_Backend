package com.synchtask.user.application.dto

import com.synchtask.user.domain.entity.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateUserDTO(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,

    val profilePictureUrl: String? = null,

    val passwordHash: String? = null,
) {

    fun toUser(existingUser: User): User {
        return existingUser.copy(
            id = existingUser.id, // Keep original ID
            name = this.name,
            email = this.email,
            passwordHash = this.passwordHash ?: existingUser.passwordHash, // Only update if provided
            profilePictureUrl = this.profilePictureUrl ?: existingUser.profilePictureUrl.orEmpty(),
            role = existingUser.role, // Keep role unchanged
            isActive = existingUser.isActive // Keep status unchanged
        )
    }
}
