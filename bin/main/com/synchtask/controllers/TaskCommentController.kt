package com.synchtask.controllers

import com.synchtask.dtos.task.TaskCommentCreateDTO
import com.synchtask.dtos.task.TaskCommentResponseDTO
import com.synchtask.services.task.TaskCommentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * **Task Comment Controller**
 *
 * API endpoints for managing comments on tasks.
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
class TaskCommentController(
    private val taskCommentService: TaskCommentService
) {

    /**
     * Adds a new comment to a task.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun addComment(
        @PathVariable taskId: Long,
        @Valid @RequestBody request: TaskCommentCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskCommentResponseDTO> {
        val userEmail = user.username
        val comment = taskCommentService.addComment(taskId, userEmail, request)
        return ResponseEntity.ok(comment)
    }

    /**
     * Lists all comments for a task.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getComments(@PathVariable taskId: Long): ResponseEntity<List<TaskCommentResponseDTO>> {
        val comments = taskCommentService.getCommentsForTask(taskId)
        return ResponseEntity.ok(comments)
    }
}
