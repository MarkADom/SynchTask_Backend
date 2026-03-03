package com.synchtask.project.application.dto


import jakarta.validation.constraints.Size
import java.time.LocalDate

data class ProjectUpdateDTO(
    @field:Size(min = 1, max = 120, message = "Project name must be between 1 and 120 characters")
    val name: String? = null,
    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,
    @field:Size(max = 50, message = "Tag must be at most 50 characters")
    val tag: String? = null,
    @field:Size(max = 20, message = "Color must be at most 20 characters")
    val color: String? = null,
    val dueDate: LocalDate? = null,
    @field:Size(max = 100, message = "Board ids must contain at most 100 items")
    val boardIds: List<Long>? = null
)
