package com.synchtask.security.application.dto

data class OpenIdConfigurationDTO(
    val issuer: String,
    val jwksUri: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val userinfoEndpoint: String,
)
