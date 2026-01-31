package com.synchtask.dtos.user

import com.synchtask.entities.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * **DTO for Updating User Information**
 *
 * This class is used to update user details, allowing changes in name, email,
 * profile picture, and optionally updating the password.
 *
 * @param name The updated name of the user.
 * @param email The updated email address of the user.
 * @param profilePictureUrl The updated profile picture URL (optional).
 * @param passwordHash The new password (optional). If not provided, the existing one is kept.
 */
data class UpdateUserDTO(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,

    val profilePictureUrl: String? = null,

    val passwordHash: String? = null,
) {
    /**
     * **Converts the DTO into an updated User entity**
     *
     * This method ensures that:
     * - The existing user ID is preserved.
     * - The password is only updated if a new one is provided.
     * - Profile picture is updated if provided, otherwise retains the current value.
     *
     * @param existingUser The current user entity from the database.
     * @return A new `User` instance with updated fields.
     */
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
