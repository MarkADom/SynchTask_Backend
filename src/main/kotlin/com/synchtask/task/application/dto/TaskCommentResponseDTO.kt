package com.synchtask.task.application.dto

import java.time.LocalDateTime

data class TaskCommentResponseDTO(
    val id: Long,
    val taskId: Long,
    val userId: Long,
    val content: String,
    val createdAt: LocalDateTime
)
