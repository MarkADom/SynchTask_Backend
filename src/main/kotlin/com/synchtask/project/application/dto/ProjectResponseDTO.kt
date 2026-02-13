package com.synchtask.project.application.dto

import com.synchtask.board.application.dto.BoardSimpleDTO
import java.time.LocalDate


data class ProjectResponseDTO(
    val id: Long,
    val name: String,
    val description: String,
    val tag: String?,
    val color: String?,
    val dueDate: LocalDate?,
    val members: List<String>,
    val boards: List<BoardSimpleDTO>
)
