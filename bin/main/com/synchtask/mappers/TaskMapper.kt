package com.synchtask.mappers

import com.synchtask.dtos.task.TaskCommentResponseDTO
import com.synchtask.dtos.task.TaskResponseDTO
import com.synchtask.entities.Task
import com.synchtask.entities.TaskComment

/**
 * **TaskMapper**
 *
 * Converts Task-related entities into DTOs.
 */
object TaskMapper {

    /**
     * **Converts a Task entity to a TaskResponseDTO**
     */
    fun toTaskResponseDTO(task: Task): TaskResponseDTO {
        return TaskResponseDTO(
            id = task.id ?: throw IllegalArgumentException("Task ID cannot be null"),
            title = task.title,
            description = task.description,
            creatorId = task.owner.id!!,
            creatorName = task.owner.name,
            assignees = task.collaborators.map { it.id!! },
            status = task.status,
            labels = task.labels.toList(),
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            boardId = task.board.id ?: throw IllegalArgumentException("Board ID cannot be null"),
            boardName = task.board.name,
            projectName = task.board.project?.name,
            priority = task.priority,
        )
    }

    /**
     * **Converts a TaskComment entity to a TaskCommentResponseDTO**
     */
    fun toTaskCommentResponseDTO(comment: TaskComment): TaskCommentResponseDTO {
        return TaskCommentResponseDTO(
            id = comment.id!!,
            taskId = comment.task.id!!,
            userId = comment.user.id!!,
            content = comment.content,
            createdAt = comment.createdAt
        )
    }
}

