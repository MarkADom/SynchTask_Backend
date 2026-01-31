package com.synchtask.dtos.board

import java.time.LocalDateTime

/**
 * DTO returned when fetching board data.
 */
data class BoardResponseDTO(
    val id: Long,
    val name: String,
    val color: String?,
    val description: String?,
    val createdAt : LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val ownerName: String,
)
