package com.synchtask.task.application.dto

import java.time.LocalDateTime

data class TaskLinkDTO(
    val id: Long?,
    val title: String,
    val url: String,
    val createdAt: LocalDateTime
)
