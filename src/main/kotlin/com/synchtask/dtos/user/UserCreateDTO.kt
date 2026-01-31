package com.synchtask.dtos.user

data class UserCreateDTO(
    val name: String,
    val email: String,
    val password: String,
    val profilePictureUrl: String? = null
)
