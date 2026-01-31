package com.synchtask.dtos.board

/**
 * DTO for creating new boards.
 */
data class BoardCreateDTO(
    val name: String,
    val color: String?,
    val description: String? = null
)
