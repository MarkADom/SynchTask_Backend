package com.synchtask.task.presentation.controller

import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.service.TaskService
import com.synchtask.user.application.service.UserService
import com.synchtask.task.application.dto.TaskAssigneeUpdateDTO
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskLabelUpdateDTO
import com.synchtask.task.application.dto.TaskResponseDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import jakarta.persistence.EntityManager
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
 * Task HTTP endpoints.
 *
 * Visibility and permission rules are enforced in the service layer.
 */
@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(TaskController::class.java)

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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
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

        // Retrieve filtered, visibility-safe tasks
        val tasks = taskService.getTasksWithFilters(
            user = userEntity,
            status = status,
            label = label,
            assigneeId = assigneeId,
            boardId = boardId,
            pageable = pageable
        )

        // Map to DTO for response
        return tasks.map { TaskResponseDTO.fromEntity(it) }
    }

    @GetMapping("/{taskId}")
    @Transactional(readOnly = true)
    fun getTaskDetail(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): TaskResponseDTO {
        val task = taskService.findTaskById(taskId)
        val userEntity = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        if (!task.canBeAccessedBy(userEntity)) {
            throw UnauthorizedAccessException("You do not have access to this task.")
        }

        return TaskResponseDTO.fromEntity(task)
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody updatedTask: TaskUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskResponseDTO> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        val updated = taskService.updateTask(taskId, updatedTask, actor)
        return ResponseEntity.ok(TaskResponseDTO.fromEntity(updated))
    }

    @PutMapping("/{taskId}/status")
    @PreAuthorize("isAuthenticated()")
    fun updateStatus(
        @PathVariable taskId: Long,
        @RequestParam status: TaskStatus,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        taskService.updateTaskStatus(
            taskId = taskId,
            newStatus = status,
            actor = actor
        )

        return ResponseEntity.ok("Task status updated successfully")
    }

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

    @PostMapping("/{taskId}/assign")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun assignCollaborator(
        @PathVariable taskId: Long,
        @RequestParam email: String,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found.")

        taskService.assignCollaborator(taskId, email, actor)
        return ResponseEntity.ok("Collaborator assigned successfully")
    }

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

    /**
     * Admin-only endpoint to clear JPA second-level cache.
     */
    @RestController
    @RequestMapping("/admin/cache")
    class CacheAdminController(
        private val entityManager: EntityManager
    ) {

        @PostMapping("/clear")
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
        fun clear(): ResponseEntity<String> {
            entityManager.entityManagerFactory.cache.evictAll()
            return ResponseEntity.ok("Cache cleared")
        }
    }
}
