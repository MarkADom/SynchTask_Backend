package com.synchtask.dtos

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponseDTO(
    val message: String,
    val error: String
)
