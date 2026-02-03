package com.synchtask.user.application.dto

data class UserCreateDTO(
    val name: String,
    val email: String,
    val password: String,
    val profilePictureUrl: String? = null
)
