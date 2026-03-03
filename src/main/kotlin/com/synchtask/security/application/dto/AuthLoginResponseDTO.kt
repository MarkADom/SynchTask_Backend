package com.synchtask.security.application.dto

import com.synchtask.user.application.dto.UserResponseDTO

data class AuthLoginResponseDTO(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponseDTO,
)
