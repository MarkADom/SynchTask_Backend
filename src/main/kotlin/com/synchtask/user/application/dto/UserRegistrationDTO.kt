package com.synchtask.user.application.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.crypto.password.PasswordEncoder

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
