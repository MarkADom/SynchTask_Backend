package com.synchtask.dtos.task

import com.synchtask.entities.TaskPriority
import com.synchtask.entities.TaskStatus

/**
 * DTO for updating an existing task.
 */
data class TaskUpdateDTO(
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val labels: List<String>? = null,
    val assignees: List<Long>? = null
)
