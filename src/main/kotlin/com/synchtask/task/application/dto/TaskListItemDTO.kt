package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import java.time.LocalDateTime

data class TaskListItemDTO(
    val id: Long,
    val title: String,
    val creatorId: Long,
    val creatorName: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val boardId: Long,
    val boardName: String,
)
