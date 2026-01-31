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
 * Task comment endpoints.
 *
 * Permission checks and task visibility rules live in the service layer.
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
class TaskCommentController(
    private val taskCommentService: TaskCommentService
) {

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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getComments(@PathVariable taskId: Long): ResponseEntity<List<TaskCommentResponseDTO>> {
        val comments = taskCommentService.getCommentsForTask(taskId)
        return ResponseEntity.ok(comments)
    }
}
