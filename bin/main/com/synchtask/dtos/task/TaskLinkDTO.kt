package com.synchtask.dtos.task

import com.synchtask.entities.TaskLink
import java.time.LocalDateTime

/**
 * DTO representing a link attached to a task.
 */
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
