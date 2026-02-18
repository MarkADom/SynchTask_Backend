package com.synchtask.task.presentation.controller

import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskAssigneeUpdateDTO
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskLabelUpdateDTO
import com.synchtask.task.application.dto.TaskListItemDTO
import com.synchtask.task.application.dto.TaskResponseDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.application.service.TaskService
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.presentation.mapper.TaskMapper
import com.synchtask.user.application.service.AuthenticatedUserService
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
    private val authenticatedUserService: AuthenticatedUserService,
) {
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createTask(@RequestBody request: TaskCreateDTO, @AuthenticationPrincipal user: UserDetails,): TaskResponseDTO {
        val creator = authenticatedUserService.requireUser(user)
        val created = taskService.createTask(creator, request)
        return TaskMapper.toResponse(created)
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
    ): Page<TaskListItemDTO> {
        val userEntity = authenticatedUserService.requireUser(user)
        val tasks =
            taskService.getTasksWithFilters(
                user = userEntity,
                status = status,
                label = label,
                assigneeId = assigneeId,
                boardId = boardId,
                pageable = pageable
            )

        return tasks.map { TaskMapper.toListItem(it) }
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    fun getTaskDetail(@PathVariable taskId: Long, @AuthenticationPrincipal user: UserDetails,): TaskResponseDTO {
        val task = taskService.findTaskById(taskId)
        val userEntity = authenticatedUserService.requireUser(user)

        if (!task.canBeAccessedBy(userEntity)) {
            throw UnauthorizedAccessException("You do not have access to this task.")
        }

        return TaskMapper.toResponse(task)
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody updatedTask: TaskUpdateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<TaskResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        val updated = taskService.updateTask(taskId, updatedTask, actor)
        return ResponseEntity.ok(TaskMapper.toResponse(updated))
    }

    @PutMapping("/{taskId}/status")
    @PreAuthorize("isAuthenticated()")
    fun updateStatus(
        @PathVariable taskId: Long,
        @RequestParam status: TaskStatus,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        val actor = authenticatedUserService.requireUser(user)

        taskService.updateTaskStatus(
            taskId = taskId,
            newStatus = status,
            actor = actor
        )

        return ResponseEntity.ok("Task status updated successfully")
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun deleteTask(@PathVariable taskId: Long, @AuthenticationPrincipal user: UserDetails,): ResponseEntity<String> {
        val userEntity = authenticatedUserService.requireUser(user)

        taskService.deleteTask(taskId, userEntity)
        return ResponseEntity.ok("Task deleted successfully")
    }

    @PostMapping("/{taskId}/assign")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun assignCollaborator(
        @PathVariable taskId: Long,
        @RequestParam email: String,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        val actor = authenticatedUserService.requireUser(user)
        taskService.assignCollaborator(taskId, email, actor)
        return ResponseEntity.ok("Collaborator assigned successfully")
    }

    @PutMapping("/{taskId}/labels")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun updateLabels(
        @PathVariable taskId: Long,
        @RequestBody request: TaskLabelUpdateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        val userEntity = authenticatedUserService.requireUser(user)

        taskService.updateTaskLabels(taskId, request.labels, userEntity)
        return ResponseEntity.ok("Labels updated successfully")
    }

    @PutMapping("/{taskId}/assignees")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun updateAssignees(
        @PathVariable taskId: Long,
        @RequestBody request: TaskAssigneeUpdateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        val userEntity = authenticatedUserService.requireUser(user)

        taskService.updateTaskAssignees(taskId, request.userIds, userEntity)
        return ResponseEntity.ok("Assignees updated successfully")
    }
}
