package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.task.presentation.mapper.TaskMapper
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskCommentService(
    private val taskCommentRepository: TaskCommentRepository,
    private val taskRepository: TaskRepository,
    private val taskMemberRepository: TaskMemberRepository,
    private val boardMemberRepository: BoardMemberRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val activityService: ActivityService
) {
    private val logger = LoggerFactory.getLogger(TaskCommentService::class.java)

    @Transactional
    fun addComment(taskId: Long, user: User, request: TaskCommentCreateDTO): TaskCommentResponseDTO {
        val task =
            taskRepository.findById(taskId)
                .orElseThrow { ResourceNotFoundException("Task not found") }

        if (!canAccessTask(task, user)) {
            throw UnauthorizedAccessException("Not allowed to comment on this task")
        }

        val comment =
            taskCommentRepository.save(
                TaskComment(
                    task = task,
                    user = user,
                    content = request.content
                )
            )

        activityService.record(
            actor = user,
            type = ActivityType.TASK_COMMENTED,
            referenceId = task.id,
            description = "Comentou na task '${task.title}'"
        )

        (resolveNotificationRecipients(task) + task.owner)
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
        val task =
            taskRepository.findById(taskId)
                .orElseThrow { ResourceNotFoundException("Task not found") }

        return taskCommentRepository
            .findByTaskOrderByCreatedAtAsc(task)
            .map { TaskMapper.toCommentResponse(it) }
    }

    private fun canAccessTask(task: com.synchtask.task.domain.entity.Task, actor: User): Boolean {
        if (actor.role == UserRole.ADMIN) return true

        val taskId = task.id ?: return false
        val actorId = actor.id ?: return false
        if (taskMemberRepository.existsByTaskIdAndUserId(taskId, actorId)) {
            return true
        }
        val boardId = task.board.id
        if (boardId != null && boardMemberRepository.existsByBoardIdAndUserId(boardId, actorId)) {
            return true
        }

        // TODO(PR4): Remove legacy task/board fallback once membership migration is complete.
        val fallback =
            task.owner.id == actorId ||
                task.collaborators.any { it.id == actorId } ||
                task.board.owner.id == actorId ||
                task.board.collaborators.any { it.id == actorId }
        if (fallback) {
            logger.warn("Using task comment legacy fallback access check for taskId={} userId={}", taskId, actorId)
        }
        return fallback
    }

    private fun resolveNotificationRecipients(task: com.synchtask.task.domain.entity.Task): Set<User> {
        val taskId = task.id ?: return task.collaborators
        val membershipUsers = taskMemberRepository.findAllByTaskId(taskId).map { it.user }.toSet()
        if (membershipUsers.isNotEmpty()) {
            return membershipUsers
        }

        // TODO(PR4): Remove legacy collaborator fallback once membership migration is complete.
        if (task.collaborators.isNotEmpty()) {
            logger.warn("Using task comment legacy fallback recipients for taskId={}", taskId)
        }
        return task.collaborators
    }

}
