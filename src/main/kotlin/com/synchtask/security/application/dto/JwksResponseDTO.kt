package com.synchtask.security.application.dto

data class JwksResponseDTO(
    val keys: List<JwkKeyDTO>,
)

data class JwkKeyDTO(
    val kty: String,
    val alg: String,
    val use: String,
    val n: String,
    val e: String,
    val kid: String,
)
