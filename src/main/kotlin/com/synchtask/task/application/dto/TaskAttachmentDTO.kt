package com.synchtask.task.application.dto

import java.time.LocalDateTime

data class TaskAttachmentDTO(
    val id: Long?,
    val fileName: String,
    val fileUrl: String,
    val uploadedAt: LocalDateTime
)
