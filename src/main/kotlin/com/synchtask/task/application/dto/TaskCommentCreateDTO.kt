package com.synchtask.task.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TaskCommentCreateDTO(
    @field:NotBlank(message = "Comment content is required")
    @field:Size(min = 1, max = 2000, message = "Comment content must be between 1 and 2000 characters")
    val content: String
)
