package com.synchtask.dtos.user

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * **User Response DTO**
 *
 * Represents the response data for user-related requests.
 *
 * @param id The unique identifier of the user.
 * @param name The user's full name.
 * @param email The user's email address.
 * @param profilePictureUrl The URL of the user's profile picture (optional).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponseDTO(
    val id: Long,
    val name: String,
    val email: String,
    val profilePictureUrl: String?
)
