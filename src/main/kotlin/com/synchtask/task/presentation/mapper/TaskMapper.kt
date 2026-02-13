package com.synchtask.task.presentation.mapper

import com.synchtask.shared.presentation.mapper.MapperSupport.requireId
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.task.application.dto.TaskResponseDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment

object TaskMapper {

    fun toTaskResponseDTO(task: Task): TaskResponseDTO =
        TaskResponseDTO(
            id = requireId(task.id, "Task"),
            title = task.title,
            description = task.description,
            creatorId = requireId(task.owner.id, "User"),
            creatorName = task.owner.name,
            assignees = task.collaborators.map { requireId(it.id, "User") },
            status = task.status,
            labels = task.labels.toList(),
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            boardId = requireId(task.board.id, "Board"),
            boardName = task.board.name,
            projectName = task.board.project?.name,
            priority = task.priority,
        )

    fun toTaskCommentResponseDTO(comment: TaskComment): TaskCommentResponseDTO =
        TaskCommentResponseDTO(
            id = requireId(comment.id, "Comment"),
            taskId = requireId(comment.task.id, "Task"),
            userId = requireId(comment.user.id, "User"),
            content = comment.content,
            createdAt = comment.createdAt
        )
    }
