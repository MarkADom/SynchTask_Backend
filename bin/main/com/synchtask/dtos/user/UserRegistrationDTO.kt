package com.synchtask.dtos.user

import com.fasterxml.jackson.annotation.JsonInclude
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * **User Registration DTO**
 *
 * Represents the data required for user registration.
 *
 * @param name The user's full name.
 * @param email The user's email address.
 * @param password The user's password (min 6 characters).
 * @param profilePictureUrl The optional URL of the user's profile picture.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // Ensures null fields are omitted in JSON responses
data class UserRegistrationDTO(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:Email(message = "Email should be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password should be at least 6 characters long")
    val password: String,

    val profilePictureUrl: String? = null
) {
    /**
     * Converts DTO to a `User` entity, encoding the password.
     *
     * @param passwordEncoder The password encoder used for hashing the password.
     * @return A `User` entity with encoded password.
     */
    fun toUser(passwordEncoder: PasswordEncoder): User {
        return User(
            name = this.name,
            email = this.email,
            passwordHash = passwordEncoder.encode(this.password),
            profilePictureUrl = this.profilePictureUrl ?: "",
            role = UserRole.USER
        )
    }
}
