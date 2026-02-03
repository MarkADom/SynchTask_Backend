package com.synchtask.shared.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponseDTO(
    val message: String,
    val error: String
)
