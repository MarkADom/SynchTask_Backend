package com.synchtask.security.application.dto

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequestDTO(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
)
