package com.synchtask.activity.application.dto

import com.synchtask.activity.domain.entity.Activity
import java.time.LocalDateTime

data class ActivityResponseDTO(
    val id: Long,
    val type: String,
    val actorEmail: String,
    val referenceId: Long?,
    val description: String?,
    val createdAt: LocalDateTime
)
