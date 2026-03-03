package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.friend.application.port.FriendshipChecker
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskMember
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

        if (!canAccessBoard(board.id, owner)) {
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
        syncTaskAssignees(savedTask, request.assignees, owner)

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
    ): Page<Task> = taskSpecificationService.findTasksByFilters(
        user,
        status,
        label,
        assigneeId,
        boardId,
        pageable
    )

    @Transactional
    fun updateTask(
        taskId: Long,
        request: TaskUpdateDTO,
        user: User,
    ): Task {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException(
                "User ${user.email} is not authorized to update task ${task.id}"
            )
        }

        task.updateDetails(
            title = request.title,
            description = request.description,
            labels = request.labels?.toSet(),
            status = request.status,
            priority = request.priority
        )

        request.assignees?.let { assigneeIds ->
            syncTaskAssignees(task, assigneeIds, user)
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
    fun updateTaskLabels(
        taskId: Long,
        labels: List<String>,
        user: User,
    ) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException(
                "You are not authorized to update labels on this task."
            )
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
    fun updateTaskAssignees(
        taskId: Long,
        userIds: List<Long>,
        user: User,
    ) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException(
                "You are not authorized to update assignees on this task."
            )
        }
        syncTaskAssignees(task, userIds, user)
        taskRepository.save(task)

        activityService.record(
            actor = user,
            type = ActivityType.TASK_ASSIGNED,
            referenceId = task.id,
            description = "Assignees updated"
        )
    }

    @Transactional
    fun updateTaskStatus(
        taskId: Long,
        newStatus: TaskStatus,
        actor: User,
    ) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, actor)) {
            throw UnauthorizedAccessException(
                "Not allowed to change task status"
            )
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
    fun assignCollaborator(
        taskId: Long,
        collaboratorEmail:
        String,
        actor: User,
    ) {
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
        val taskIdValue = task.id ?: throw ResourceNotFoundException("Task not found with ID: $taskId")
        val existingMembership = taskMemberRepository.findByTaskIdAndUserId(taskIdValue, collaborator.id!!)
        if (existingMembership == null) {
            taskMemberRepository.save(
                TaskMember(
                    task = task,
                    user = collaborator,
                    role = MembershipRole.COLLABORATOR,
                    createdByUser = actor
                )
            )
        }

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
    fun deleteTask(
        taskId: Long,
        user: User,
    ) {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException(
                "User ${user.email} is not authorized to delete this task."
            )
        }

        taskRepository.delete(task)
        logger.info("Task '${task.title}' deleted by ${user.email}")
    }

    private fun canAccessBoard(
        boardId: Long?,
        actor: User,
    ): Boolean {
        if (actor.role == UserRole.ADMIN) return true
        val safeBoardId = boardId ?: return false
        val actorId = actor.id ?: return false

        return boardMemberRepository.existsByBoardIdAndUserId(safeBoardId, actorId)
    }

    private fun canEditTask(
        task: Task,
        actor: User,
    ): Boolean =
        hasTaskAccess(task, actor, taskMemberRepository, boardMemberRepository)

    private fun resolveTaskCollaborators(task: Task): Set<User> =
        resolveTaskMembershipUsers(task, taskMemberRepository)

    private fun syncTaskAssignees(task: Task, assigneeIds: List<Long>, actor: User) {
        val usersById = userRepository.findAllById(assigneeIds).associateBy { it.id }
        if (usersById.size != assigneeIds.size) {
            throw ResourceNotFoundException("Some users not found")
        }

        val taskId = checkNotNull(task.id) { "Task not found" }
        val existingMemberships = taskMemberRepository.findAllByTaskId(taskId)
        val ownerMemberships = existingMemberships.filter { it.role == MembershipRole.OWNER }
        taskMemberRepository.deleteAll(existingMemberships.filter { it.role != MembershipRole.OWNER })

        val ownerIds = ownerMemberships.mapNotNull { it.user.id }.toSet()
        val assigneeMemberships = assigneeIds
            .distinct()
            .filter { it !in ownerIds }
            .map { assigneeId ->
                TaskMember(
                    task = task,
                    user = usersById.getValue(assigneeId),
                    role = MembershipRole.COLLABORATOR,
                    createdByUser = actor
                )
            }
        taskMemberRepository.saveAll(assigneeMemberships)

        if (ownerIds.isEmpty()) {
            taskMemberRepository.save(
                TaskMember(
                    task = task,
                    user = task.owner,
                    role = MembershipRole.OWNER,
                    createdByUser = actor
                )
            )
        }
    }
}
