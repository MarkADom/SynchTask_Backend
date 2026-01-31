package com.synchtask.dtos

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * **Error Response DTO**
 *
 * Standard structure for returning error messages in API responses.
 *
 * @param message A human-readable error description.
 * @param error The type of error (e.g., `"Unauthorized"`, `"Not Found"`).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponseDTO(
    val message: String,
    val error: String
)
