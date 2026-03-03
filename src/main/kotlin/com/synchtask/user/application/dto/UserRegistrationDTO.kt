package com.synchtask.user.application.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL) // Ensures null fields are omitted in JSON responses
data class UserRegistrationDTO(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    val name: String,
    @field:Email(message = "Email should be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, max = 128, message = "Password should be between 6 and 128 characters long")
    val password: String,
    @field:Size(max = 512, message = "Profile picture URL must be at most 512 characters")
    val profilePictureUrl: String? = null
)
