package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskComment
import java.time.LocalDateTime

data class TaskCommentCreateDTO(
    val content: String
)

data class TaskCommentResponseDTO(
    val id: Long,
    val taskId: Long,
    val userId: Long,
    val content: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun fromEntity(comment: TaskComment): TaskCommentResponseDTO {
            return TaskCommentResponseDTO(
                id = requireNotNull(comment.id) { "Comment ID cannot be null" },
                taskId = requireNotNull(comment.task.id) { "Task ID cannot be null" },
                userId = requireNotNull(comment.user.id) { "User ID cannot be null" },
                content = comment.content,
                createdAt = comment.createdAt
            )
        }

    }
}
