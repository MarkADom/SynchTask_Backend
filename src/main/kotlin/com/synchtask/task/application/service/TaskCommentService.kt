package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.presentation.mapper.TaskMapper
import com.synchtask.user.domain.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskCommentService(
    private val taskCommentRepository: TaskCommentRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val activityService: ActivityService
) {

    private val logger = LoggerFactory.getLogger(TaskCommentService::class.java)

    @Transactional
    fun addComment(
        taskId: Long,
        user: User,
        request: TaskCommentCreateDTO
    ): TaskCommentResponseDTO {

        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found") }

        if (!task.canBeAccessedBy(user)) {
            throw UnauthorizedAccessException("Not allowed to comment on this task")
        }

        val comment = taskCommentRepository.save(
            TaskComment(
                task = task,
                user = user,
                content = request.content
            )
        )

        // Activity
        activityService.record(
            actor = user,
            type = ActivityType.TASK_COMMENTED,
            referenceId = task.id,
            description = "Comentou na task '${task.title}'"
        )

        // Notifications
        (task.collaborators + task.owner)
            .distinctBy { it.id }
            .filter { it.id != user.id }
            .forEach {
                notificationService.sendNotification(
                    userEmail = it.email,
                    message = "New comment on task '${task.title}'",
                    type = NotificationType.TASK_UPDATE,
                    groupId = task.id
                )
            }

        logger.info("Comment added by ${user.email} on task '${task.title}'")
        return TaskMapper.toCommentResponse(comment)
    }

    @Transactional(readOnly = true)
    fun getCommentsForTask(taskId: Long): List<TaskCommentResponseDTO> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found") }

        return taskCommentRepository
            .findByTaskOrderByCreatedAtAsc(task)
            .map { TaskMapper.toCommentResponse(it) }
    }
}
