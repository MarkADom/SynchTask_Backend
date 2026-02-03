package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskLink
import java.time.LocalDateTime

data class TaskLinkDTO(
    val id: Long?,
    val title: String,
    val url: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun fromEntity(link: TaskLink): TaskLinkDTO {
            return TaskLinkDTO(
                id = link.id,
                title = link.title,
                url = link.url,
                createdAt = link.createdAt
            )
        }
    }
}
