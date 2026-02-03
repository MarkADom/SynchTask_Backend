package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus

data class TaskUpdateDTO(
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val labels: List<String>? = null,
    val assignees: List<Long>? = null
)
