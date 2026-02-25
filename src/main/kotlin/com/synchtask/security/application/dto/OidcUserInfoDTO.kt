package com.synchtask.security.application.dto

data class OidcUserInfoDTO(
    val email: String,
    val name: String,
    val roles: List<String>,
)
