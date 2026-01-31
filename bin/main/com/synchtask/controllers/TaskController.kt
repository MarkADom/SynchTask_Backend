package com.synchtask.controllers

import com.synchtask.dtos.task.*
import com.synchtask.entities.TaskStatus
import com.synchtask.entities.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.services.task.TaskService
import com.synchtask.services.user.UserService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/**
 * **Task Controller**
 *
 * Provides secured REST endpoints for task management,
 * including creation, retrieval, update, deletion, and collaborator actions.
 */
@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(TaskController::class.java)

    /**
     * Creates a new task for a board the user has access to.
     */
    @PostMapping
    fun createTask(
        @RequestBody request: TaskCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): TaskResponseDTO {
        val creator = userService.getUserByEmail(user.username)
            ?: throw UnauthorizedAccessException("User not authenticated.")
        val created = taskService.createTask(creator, request)
        return TaskResponseDTO.fromEntity(created)
    }

    /**
     * Retrieves a paginated list of tasks filtered by optional criteria.
     */
    @GetMapping
    fun getTasks(
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam(required = false) status: TaskStatus?,
        @RequestParam(required = false) label: String?,
        @RequestParam(required = false) assigneeId: Long?,
        @RequestParam(required = false) boardId: Long?,
        @PageableDefault(size = 20, sort = ["createdAt"]) pageable: Pageable,
    ): Page<TaskResponseDTO> {
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found: ${user.username}")

        val tasks = taskService.getTasksWithFilters(userEntity, status, label, assigneeId, boardId, pageable)
        return tasks.map { TaskResponseDTO.fromEntity(it) }
    }

    /**
     * Returns detailed information about a specific task,
     * if the user is authorized to view it.
     */
    @GetMapping("/{taskId}")
    @Transactional(readOnly = true)
    fun getTaskDetail(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): TaskResponseDTO {
        val task = taskService.findTaskById(taskId)
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        if (!taskService.canAccessTask(task, userEntity)) {
            throw UnauthorizedAccessException("You do not have access to this task.")
        }

        return TaskResponseDTO.fromEntity(task)
    }

    /**
     * Updates a task's title, description, status, priority or collaborators.
     */
    @PutMapping("/{taskId}")
    fun updateTask(
        @PathVariable taskId: Long,
        @Valid @RequestBody updatedTask: TaskUpdateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<TaskResponseDTO> {
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        val updated = taskService.updateTask(taskId, updatedTask, userEntity)
        return ResponseEntity.ok(TaskResponseDTO.fromEntity(updated))
    }

    /**
     * Deletes a task. Requires OWNER or ADMIN role.
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun deleteTask(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        taskService.deleteTask(taskId, userEntity)
        return ResponseEntity.ok("Task deleted successfully")
    }

    /**
     * Assigns a user to the task by email.
     */
    @PostMapping("/{taskId}/assign")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun assignCollaborator(
        @PathVariable taskId: Long,
        @RequestParam email: String,
    ): ResponseEntity<String> {
        taskService.assignCollaborator(taskId, email)
        return ResponseEntity.ok("Collaborator assigned successfully")
    }

    /**
     * Replaces the list of labels on the task.
     */
    @PutMapping("/{taskId}/labels")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun updateLabels(
        @PathVariable taskId: Long,
        @RequestBody request: TaskLabelUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        taskService.updateTaskLabels(taskId, request.labels, userEntity)
        return ResponseEntity.ok("Labels updated successfully")
    }

    /**
     * Updates the assigned users of the task.
     */
    @PutMapping("/{taskId}/assignees")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun updateAssignees(
        @PathVariable taskId: Long,
        @RequestBody request: TaskAssigneeUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        taskService.updateTaskAssignees(taskId, request.userIds, userEntity)
        return ResponseEntity.ok("Assignees updated successfully")
    }
}
