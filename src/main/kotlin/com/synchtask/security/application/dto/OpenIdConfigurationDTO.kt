package com.synchtask.security.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenIdConfigurationDTO(
    val issuer: String,
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("userinfo_endpoint")
    val userinfoEndpoint: String,
)
