package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.friend.application.port.FriendshipChecker
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.task.presentation.mapper.TaskMapper
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.websocket.application.service.TaskWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskMemberRepository: TaskMemberRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val taskWebSocketService: TaskWebSocketService,
    private val boardRepository: BoardRepository,
    private val boardMemberRepository: BoardMemberRepository,
    private val taskSpecificationService: TaskSpecificationService,
    private val friendshipChecker: FriendshipChecker,
    private val activityService: ActivityService,
) {
    private val logger = LoggerFactory.getLogger(TaskService::class.java)

    @Transactional
    fun createTask(owner: User, request: TaskCreateDTO): Task {
        val board =
            boardRepository.findById(request.boardId)
                .orElseThrow { ResourceNotFoundException("Board not found: ${request.boardId}") }

        if (!canAccessBoard(
                board.id,
                owner,
                board.owner.id == owner.id,
                board.collaborators.any { it.id == owner.id })
        ) {
            throw UnauthorizedAccessException(
                "User ${owner.email} is not allowed to create tasks in board ${board.id}."
            )
        }

        val newTask =
            Task(
                owner = owner,
                title = request.title,
                description = request.description,
                labels = request.labels.toMutableSet(),
                status = request.status,
                board = board,
                priority = request.priority,
            )

        val savedTask = taskRepository.save(newTask)

        activityService.record(
            actor = owner,
            type = ActivityType.TASK_CREATED,
            referenceId = savedTask.id,
            description = "Task '${savedTask.title}' criada"
        )

        logger.info("Task '${savedTask.title}' created by ${owner.email}")
        return savedTask
    }

    fun findTaskById(taskId: Long): Task = taskRepository.findById(taskId)
        .orElseThrow { ResourceNotFoundException("Task not found with ID: $taskId") }

    fun getTasksWithFilters(
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?,
        pageable: Pageable,
    ): Page<Task> = taskSpecificationService.findTasksByFilters(user, status, label, assigneeId, boardId, pageable)

    @Transactional
    fun updateTask(taskId: Long, request: TaskUpdateDTO, user: User): Task {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("User ${user.email} is not authorized to update task ${task.id}")
        }

        task.updateDetails(
            title = request.title,
            description = request.description,
            labels = request.labels?.toSet(),
            status = request.status,
            priority = request.priority
        )

        request.assignees?.let { assigneeIds ->
            val collaborators = userRepository.findAllById(assigneeIds).toSet()
            if (collaborators.size != assigneeIds.size) {
                throw ResourceNotFoundException("Some users not found")
            }
            task.collaborators.clear()
            task.collaborators.addAll(collaborators)
        }

        val updated = taskRepository.save(task)

        activityService.record(
            actor = user,
            type = ActivityType.TASK_UPDATED,
            referenceId = updated.id,
            description = "Task '${updated.title}' atualizada"
        )

        taskWebSocketService.sendTaskUpdate(TaskMapper.toResponse(updated))
        return updated
    }

    @Transactional
    fun updateTaskLabels(taskId: Long, labels: List<String>, user: User) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("You are not authorized to update labels on this task.")
        }

        task.labels = labels.toMutableSet()
        taskRepository.save(task)

        activityService.record(
            actor = user,
            type = ActivityType.TASK_UPDATED,
            referenceId = task.id,
            description = "Labels updated"
        )
    }

    @Transactional
    fun updateTaskAssignees(taskId: Long, userIds: List<Long>, user: User) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("You are not authorized to update assignees on this task.")
        }

        val assignees = userRepository.findAllById(userIds).toMutableSet()
        if (assignees.size != userIds.size) {
            throw ResourceNotFoundException("Some users not found")
        }

        task.collaborators.clear()
        task.collaborators.addAll(assignees)
        taskRepository.save(task)

        activityService.record(
            actor = user,
            type = ActivityType.TASK_ASSIGNED,
            referenceId = task.id,
            description = "Assignees updated"
        )
    }

    @Transactional
    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus, actor: User) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, actor)) {
            throw UnauthorizedAccessException("Not allowed to change task status")
        }

        task.changeStatus(newStatus)
        taskRepository.save(task)

        activityService.record(
            actor = actor,
            type = ActivityType.TASK_STATUS_CHANGED,
            referenceId = task.id,
            description = "Status alterado para $newStatus"
        )

        resolveTaskCollaborators(task).forEach {
            notificationService.sendNotification(
                userEmail = it.email,
                message = "Task '${task.title}' status updated to: $newStatus.",
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }

        taskWebSocketService.sendTaskUpdate(TaskMapper.toResponse(task))
    }

    @Transactional
    fun assignCollaborator(taskId: Long, collaboratorEmail: String, actor: User) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, actor)) {
            throw UnauthorizedAccessException("Not allowed to assign collaborators")
        }

        val collaborator =
            userRepository.findByEmail(collaboratorEmail)
                .orElseThrow { ResourceNotFoundException("User not found: $collaboratorEmail") }

        if (!friendshipChecker.areFriends(task.owner.id!!, collaborator.id!!)) {
            throw UnauthorizedAccessException("You can only assign friends as collaborators.")
        }

        if (!task.addCollaborator(collaborator)) {
            return
        }

        taskRepository.save(task)

        activityService.record(
            actor = actor,
            type = ActivityType.TASK_ASSIGNED,
            referenceId = task.id,
            description = "Colaborador ${collaborator.email} atribuído"
        )

        notificationService.sendNotification(
            userEmail = collaborator.email,
            message = "You have been assigned to the task: '${task.title}'.",
            type = NotificationType.TASK_UPDATE,
            groupId = task.id
        )
    }

    @Transactional
    fun deleteTask(taskId: Long, user: User) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("User ${user.email} is not authorized to delete this task.")
        }

        taskRepository.delete(task)
        logger.info("Task '${task.title}' deleted by ${user.email}")
    }

    private fun canEditTask(task: Task, actor: User): Boolean {
        if (actor.role == UserRole.ADMIN) return true

        val taskId = task.id ?: return false
        val actorId = actor.id ?: return false
        if (taskMemberRepository.existsByTaskIdAndUserId(taskId, actorId)) {
            return true
        }
        if (canAccessBoard(
                task.board.id,
                actor,
                task.board.owner.id == actorId,
                task.board.collaborators.any { it.id == actorId })
        ) {
            return true
        }

        // TODO(PR4): Remove legacy collaborator fallback once membership migration is complete.
        val fallbackResult = task.owner.id == actorId || task.collaborators.any { it.id == actorId }
        if (fallbackResult) {
            logger.warn("Using task legacy fallback edit check for taskId={} userId={}", taskId, actorId)
        }
        return fallbackResult
    }

    private fun canAccessBoard(
        boardId: Long?,
        actor: User,
        legacyOwner: Boolean,
        legacyCollaborator: Boolean,
    ): Boolean {
        if (actor.role == UserRole.ADMIN) return true
        val safeBoardId = boardId ?: return false
        val actorId = actor.id ?: return false
        if (boardMemberRepository.existsByBoardIdAndUserId(safeBoardId, actorId)) {
            return true
        }

        // TODO(PR4): Remove legacy board fallback once membership migration is complete.
        val fallback = legacyOwner || legacyCollaborator
        if (fallback) {
            logger.warn("Using board legacy fallback access check for boardId={} userId={}", safeBoardId, actorId)
        }
        return fallback
    }

    private fun resolveTaskCollaborators(task: Task): Set<User> {
        val taskId = task.id ?: return task.collaborators
        val taskMembershipUsers = taskMemberRepository.findAllByTaskId(taskId).map { it.user }.toSet()
        if (taskMembershipUsers.isNotEmpty()) {
            return taskMembershipUsers
        }

        if (task.collaborators.isNotEmpty()) {
            logger.warn("Using task legacy fallback collaborators for notifications taskId={}", taskId)
        }
        return task.collaborators
    }
}
