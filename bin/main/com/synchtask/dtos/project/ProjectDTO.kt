package com.synchtask.dtos.project

import com.synchtask.dtos.board.BoardSimpleDTO
import com.synchtask.entities.Project
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

/**
 * DTO used to create a project.
 */
data class ProjectCreateDTO(
    @field:NotBlank(message = "Project name is required")
    val name: String,
    val description: String = "",
    val tag: String? = null,
    val color: String? = null,
    val dueDate: LocalDate? = null,
    val boardIds: List<Long> = emptyList()
)
/**
 * DTO used to return full project data (with assigned boards).
 */
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
/**
 * Entity → DTO mapping function for Project.
 */
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
/**
 * DTO used for updating a project.
 * All fields are optional to allow partial updates.
 */
data class ProjectUpdateDTO(
    val name: String? = null,
    val description: String? = null,
    val tag: String? = null,
    val color: String? = null,
    val dueDate: LocalDate? = null,
    val boardIds: List<Long>? = null
)


