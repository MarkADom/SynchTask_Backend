package com.synchtask.task.presentation.controller

import com.synchtask.task.application.dto.TaskAttachmentDTO
import com.synchtask.task.application.dto.TaskLinkDTO
import com.synchtask.task.application.service.TaskAttachmentService
import com.synchtask.task.application.service.TaskLinkService
import com.synchtask.user.application.service.AuthenticatedUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Task-related resources (links and attachments).
 *
 * Authorization and ownership checks are enforced in the service layer.
 */
@RestController
@RequestMapping("/tasks/{taskId}")
class TaskResourceController(
    private val taskLinkService: TaskLinkService,
    private val taskAttachmentService: TaskAttachmentService,
    private val authenticatedUserService: AuthenticatedUserService
) {
    // LINKS

    @PostMapping("/links")
    @PreAuthorize("isAuthenticated()")
    fun addLink(
        @PathVariable taskId: Long,
        @RequestParam title: String,
        @RequestParam url: String,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskLinkDTO> {
        val actor = authenticatedUserService.requireUser(user)
        val result = taskLinkService.addLink(taskId, title, url, actor)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/links")
    @PreAuthorize("isAuthenticated()")
    fun listLinks(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskLinkDTO>> {
        val actor = authenticatedUserService.requireUser(user)
        return ResponseEntity.ok(
            taskLinkService.listLinks(taskId, actor)
        )
    }

    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("isAuthenticated()")
    fun deleteLink(
        @PathVariable taskId: Long,
        @PathVariable linkId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {
        val actor = authenticatedUserService.requireUser(user)
        taskLinkService.removeLink(taskId, linkId, actor)
        return ResponseEntity.noContent().build()
    }

    // ATTACHMENTS

    @PostMapping("/attachments")
    @PreAuthorize("isAuthenticated()")
    fun uploadFile(
        @PathVariable taskId: Long,
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskAttachmentDTO> {
        val actor = authenticatedUserService.requireUser(user)
        val result = taskAttachmentService.uploadFile(taskId, file, actor)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/attachments")
    @PreAuthorize("isAuthenticated()")
    fun listAttachments(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskAttachmentDTO>> {
        val actor = authenticatedUserService.requireUser(user)
        return ResponseEntity.ok(
            taskAttachmentService.listAttachments(taskId, actor)
        )
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    fun deleteAttachment(
        @PathVariable taskId: Long,
        @PathVariable attachmentId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {
        val actor = authenticatedUserService.requireUser(user)
        taskAttachmentService.deleteAttachment(taskId, attachmentId, actor)
        return ResponseEntity.noContent().build()
    }
}
