package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import jakarta.validation.constraints.Size

data class TaskUpdateDTO(
    @field:Size(min = 1, max = 200, message = "Task title must be between 1 and 200 characters")
    val title: String? = null,
    @field:Size(max = 5000, message = "Task description must be at most 5000 characters")
    val description: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    @field:Size(max = 50, message = "Labels must contain at most 50 items")
    val labels: List<String>? = null,
    @field:Size(max = 100, message = "Assignees must contain at most 100 items")
    val assignees: List<Long>? = null
)
