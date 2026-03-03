package com.synchtask.board.application.dto

import java.time.LocalDateTime

data class BoardResponseDTO(
    val id: Long,
    val name: String,
    val color: String?,
    val description: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val ownerName: String,
)
