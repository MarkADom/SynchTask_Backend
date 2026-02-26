package com.synchtask.security.application.dto

import jakarta.validation.constraints.NotBlank

data class TokenPairDTO(
    @field:NotBlank
    val accessToken: String,
    val refreshToken: String? = null,
)
