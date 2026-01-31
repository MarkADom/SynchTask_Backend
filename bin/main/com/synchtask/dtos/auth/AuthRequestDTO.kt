package com.synchtask.dtos.auth

/**
 * **Authentication Request DTO**
 *
 * Represents a login request.
 */
data class AuthRequestDTO(
    val email: String,
    val password: String
)
