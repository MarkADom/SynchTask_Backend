package com.synchtask.project.application.dto

import com.synchtask.board.application.dto.BoardSimpleDTO
import com.synchtask.project.domain.entity.Project
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

fun Project.toResponseDTO() = ProjectResponseDTO(
    id = this.id!!,
    name = this.name,
    description = this.description,
    tag = this.tag,
    color = this.color,
    dueDate = this.dueDate,
    members = this.members.map { it.email },
    boards = this.boards.map { BoardSimpleDTO(it.id!!, it.name) }
)

