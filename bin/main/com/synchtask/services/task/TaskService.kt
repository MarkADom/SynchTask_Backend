package com.synchtask.services.task

import com.synchtask.dtos.task.TaskCreateDTO
import com.synchtask.dtos.task.TaskResponseDTO
import com.synchtask.dtos.task.TaskUpdateDTO
import com.synchtask.entities.NotificationType
import com.synchtask.entities.Task
import com.synchtask.entities.TaskPriority
import com.synchtask.entities.TaskStatus
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.repositories.BoardRepository
import com.synchtask.repositories.TaskRepository
import com.synchtask.repositories.UserRepository
import com.synchtask.services.notification.NotificationService
import com.synchtask.websocket.TaskWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * **Task Service**
 *
 * Provides business logic for task operations such as creation, updates, assignment,
 * label and status changes, collaborator management, and secure deletion.
 * Ensures access control based on ownership, role, or collaboration.
 */
@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val taskWebSocketService: TaskWebSocketService,
    private val boardRepository: BoardRepository,
    private val taskSpecificationService: TaskSpecificationService
) {

    private val logger = LoggerFactory.getLogger(TaskService::class.java)

    /**
     * Creates a new task within a board.
     *
     * @param owner The user creating the task.
     * @param request The task creation request data.
     * @return The created Task entity.
     * @throws UnauthorizedAccessException if the user cannot access the board.
     */
    @Transactional
    fun createTask(owner: User, request: TaskCreateDTO): Task {
        val board = boardRepository.findById(request.boardId)
            .orElseThrow { ResourceNotFoundException("Board not found: ${request.boardId}") }

        if (!board.hasAccess(owner)) {
            throw UnauthorizedAccessException("User ${owner.email} is not allowed to create tasks in board ${board.id}.")
        }

        val newTask = Task(
            owner = owner,
            title = request.title,
            description = request.description,
            labels = request.labels.toMutableSet(),
            status = request.status,
            board = board,
            priority = request.priority,
        )

        val savedTask = taskRepository.save(newTask)
        logger.info("Task '${newTask.title}' created by ${owner.email}")
        return savedTask
    }

    /**
     * Retrieves a task by ID or throws an exception.
     */
    fun findTaskById(taskId: Long): Task =
        taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID: $taskId") }

    /**
     * Returns a full task object for detail view.
     */
    fun getTaskDetail(taskId: Long): Task = findTaskById(taskId)

    /**
     * Lists all tasks accessible by the given user.
     */
    fun getTasksForUser(user: User): List<Task> =
        taskRepository.findTasksByUser(user)

    /**
     * Filters and paginates tasks based on parameters and user visibility.
     */
    fun getTasksWithFilters(
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?,
        pageable: Pageable,
    ): Page<Task> {
        return taskSpecificationService.findTasksByFilters(user, status, label, assigneeId, boardId, pageable)
    }

    /**
     * Updates a task. Only the owner or an ADMIN can perform this operation.
     */
    @Transactional
    fun updateTask(taskId: Long, request: TaskUpdateDTO, user: User): Task {
        val task = findTaskById(taskId)

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("User ${user.email} is not authorized to update task ${task.id}")
        }

        request.title?.let { task.title = it }
        request.description?.let { task.description = it }
        request.labels?.let { task.labels = it.toMutableSet() }
        request.status?.let { task.status = it }
        request.priority?.let { task.priority = it }

        request.assignees?.let { assigneeIds ->
            val collaborators = userRepository.findAllById(assigneeIds).toMutableSet()
            if (collaborators.size != assigneeIds.size) {
                val foundIds = collaborators.mapNotNull { it.id }.toSet()
                val missing = assigneeIds.filter { it !in foundIds }
                throw ResourceNotFoundException("Some users not found: $missing")
            }

            task.collaborators.clear()
            task.collaborators.addAll(collaborators)
        }

        markUpdated(task)
        val updated = taskRepository.save(task)
        taskWebSocketService.sendTaskUpdate(TaskResponseDTO.fromEntity(updated))
        logger.info("Task '${task.title}' updated and WebSocket notification sent.")
        return updated
    }

    /**
     * Updates task labels. Only owner or ADMIN can perform this action.
     */
    @Transactional
    fun updateTaskLabels(taskId: Long, labels: List<String>, user: User) {
        val task = findTaskById(taskId)
        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("You are not authorized to update labels on this task.")
        }

        task.labels = labels.toMutableSet()
        markUpdated(task)
        taskRepository.save(task)
        logger.info("Updated labels for task '${task.title}': $labels")
    }

    /**
     * Updates task assignees. Only owner or ADMIN can perform this action.
     */
    @Transactional
    fun updateTaskAssignees(taskId: Long, userIds: List<Long>, user: User) {
        val task = findTaskById(taskId)
        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("You are not authorized to update assignees on this task.")
        }

        val assignees = userRepository.findAllById(userIds).toMutableSet()
        if (assignees.size != userIds.size) {
            val foundIds = assignees.map { it.id }.toSet()
            val missing = userIds.filter { it !in foundIds }
            throw ResourceNotFoundException("Some users not found: $missing")
        }

        task.collaborators.clear()
        task.collaborators.addAll(assignees)
        markUpdated(task)
        taskRepository.save(task)
        logger.info("Updated assignees for task '${task.title}': ${userIds.joinToString()}")
    }

    /**
     * Updates the status of a task and sends notifications to collaborators.
     */
    @Transactional
    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus) {
        val task = findTaskById(taskId)

        if (task.status == newStatus) {
            logger.warn("Task '${task.title}' is already in status: $newStatus.")
            return
        }

        task.status = newStatus
        markUpdated(task)
        taskRepository.save(task)

        task.collaborators.forEach {
            notificationService.sendNotification(
                userEmail = it.email,
                message = "Task '${task.title}' status updated to: $newStatus.",
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }

        taskWebSocketService.sendTaskUpdate(TaskResponseDTO.fromEntity(task))
        logger.info("Task '${task.title}' status updated to '$newStatus' and notification sent.")
    }

    /**
     * Assigns a collaborator to the task and notifies them.
     */
    @Transactional
    fun assignCollaborator(taskId: Long, collaboratorEmail: String) {
        val task = findTaskById(taskId)
        val collaborator = userRepository.findByEmail(collaboratorEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $collaboratorEmail") }

        if (task.collaborators.contains(collaborator)) {
            logger.warn("User ${collaborator.email} is already assigned to task '${task.title}'.")
            return
        }

        task.collaborators.add(collaborator)
        markUpdated(task)
        taskRepository.save(task)

        notificationService.sendNotification(
            userEmail = collaborator.email,
            message = "You have been assigned to the task: '${task.title}'.",
            type = NotificationType.TASK_UPDATE,
            groupId = task.id
        )

        logger.info("User ${collaborator.email} assigned to task '${task.title}'.")
    }

    /**
     * Deletes a task. Only the owner or an ADMIN can delete it.
     */
    @Transactional
    fun deleteTask(taskId: Long, user: User) {
        val task = findTaskById(taskId)
        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException("User ${user.email} is not authorized to delete this task.")
        }

        taskRepository.delete(task)
        logger.info("Task '${task.title}' (ID: ${task.id}) deleted by ${user.email}")
    }

    /**
     * Determines if the user can view the task.
     * Admins, owners and collaborators are allowed.
     */
    fun canAccessTask(task: Task, user: User): Boolean {
        return user.role == UserRole.ADMIN ||
                user.role == UserRole.OWNER ||
                task.owner.id == user.id ||
                task.collaborators.any { it.id == user.id }
    }

    /**
     * Determines if the user can modify the task.
     * Admins and owners only.
     */
    private fun canEditTask(task: Task, user: User): Boolean {
        return user.role == UserRole.ADMIN || task.owner.id == user.id
    }

    private fun markUpdated(task: Task) {
        task.updatedAt = LocalDateTime.now()
    }
}
