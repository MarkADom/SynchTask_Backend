package com.synchtask.user.application.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_EMPTY) // Omits empty fields from JSON responses
data class UserLoginDTO(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 1, max = 128, message = "Password must be between 1 and 128 characters")
    val password: String
)
