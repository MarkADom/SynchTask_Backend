package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class TaskCreateDTO(
    @field:NotBlank(message = "Task title is required")
    @field:Size(min = 1, max = 200, message = "Task title must be between 1 and 200 characters")
    val title: String,
    @field:Size(max = 5000, message = "Task description must be at most 5000 characters")
    val description: String,
    @field:Size(max = 100, message = "Assignees must contain at most 100 items")
    val assignees: List<Long> = emptyList(),
    @field:Size(max = 50, message = "Labels must contain at most 50 items")
    val labels: List<String> = emptyList(),
    @field:Positive(message = "Board id must be positive")
    val boardId: Long,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MID
)
