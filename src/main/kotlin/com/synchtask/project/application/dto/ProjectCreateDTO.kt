package com.synchtask.project.application.dto

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
