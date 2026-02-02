package com.synchtask.user.application.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponseDTO(
    val id: Long,
    val name: String,
    val email: String,
    val profilePictureUrl: String?
)
