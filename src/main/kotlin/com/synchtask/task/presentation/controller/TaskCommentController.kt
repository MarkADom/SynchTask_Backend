package com.synchtask.task.presentation.controller

import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.task.application.service.TaskCommentService
import com.synchtask.user.application.service.AuthenticatedUserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * Task comment endpoints.
 *
 * Permission checks and task visibility rules live in the service layer.
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
class TaskCommentController(
    private val taskCommentService: TaskCommentService,
    private val authenticatedUserService: AuthenticatedUserService
) {
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun addComment(
        @PathVariable taskId: Long,
        @Valid @RequestBody request: TaskCommentCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskCommentResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)
        val comment =
            taskCommentService.addComment(
                taskId = taskId,
                user = actor,
                request = request
            )
        return ResponseEntity.ok(comment)
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getComments(@PathVariable taskId: Long): ResponseEntity<List<TaskCommentResponseDTO>> {
        val comments = taskCommentService.getCommentsForTask(taskId)
        return ResponseEntity.ok(comments)
    }
}
