package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import java.time.LocalDateTime

data class TaskResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val creatorId: Long,
    val creatorName: String,
    val assignees: List<Long>,
    val status: TaskStatus,
    val labels: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val boardId: Long,
    val boardName: String?,
    val projectName: String?,
    val priority: TaskPriority,
)
