package com.synchtask.user.application.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@JsonInclude(JsonInclude.Include.NON_EMPTY) // Omits empty fields from JSON responses
data class UserLoginDTO(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)
