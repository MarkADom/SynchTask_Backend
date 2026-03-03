package com.synchtask.board.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class BoardCreateDTO(
    @field:NotBlank(message = "Board name is required")
    @field:Size(min = 1, max = 120, message = "Board name must be between 1 and 120 characters")
    val name: String,
    @field:Size(max = 20, message = "Color must be at most 20 characters")
    val color: String?,
    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null
)
