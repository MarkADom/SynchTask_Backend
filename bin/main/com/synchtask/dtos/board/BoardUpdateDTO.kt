package com.synchtask.dtos.board

/**
 * DTO for updating boards.
 */
data class BoardUpdateDTO(
    val name: String,
    val color: String?,
    val description: String? = null
)
