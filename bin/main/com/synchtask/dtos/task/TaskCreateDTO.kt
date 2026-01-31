package com.synchtask.dtos.task

import com.synchtask.entities.Task
import com.synchtask.entities.TaskPriority
import com.synchtask.entities.TaskStatus
import java.time.LocalDateTime

/**
 * **Task Creation DTO**
 */
data class TaskCreateDTO(
    val title: String,
    val description: String,
    val assignees: List<Long> = emptyList(),
    val labels: List<String> = emptyList(),
    val boardId: Long,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MID
)

/**
 * **Task Response DTO**
 */
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
) {
    companion object {
        fun fromEntity(task: Task): TaskResponseDTO {
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
                boardId = task.board.id!!,
                boardName = task.board.name,
                projectName = task.board.project?.name,
                priority = task.priority,
            )
        }
    }
}
