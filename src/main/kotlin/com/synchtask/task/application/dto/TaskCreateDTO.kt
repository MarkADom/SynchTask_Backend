package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus

data class TaskCreateDTO(
    val title: String,
    val description: String,
    val assignees: List<Long> = emptyList(),
    val labels: List<String> = emptyList(),
    val boardId: Long,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MID
)
