package com.synchtask.project.application.dto

import com.synchtask.board.application.dto.BoardSimpleDTO
import com.synchtask.project.domain.entity.Project
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class ProjectCreateDTO(
    @field:NotBlank(message = "Project name is required")
    val name: String,
    val description: String = "",
    val tag: String? = null,
    val color: String? = null,
    val dueDate: LocalDate? = null,
    val boardIds: List<Long> = emptyList()
)

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

data class ProjectUpdateDTO(
    val name: String? = null,
    val description: String? = null,
    val tag: String? = null,
    val color: String? = null,
    val dueDate: LocalDate? = null,
    val boardIds: List<Long>? = null
)
