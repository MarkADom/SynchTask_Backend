package com.synchtask.task.presentation.controller

import com.synchtask.task.application.dto.TaskAttachmentDTO
import com.synchtask.task.application.dto.TaskLinkDTO
import com.synchtask.task.application.service.TaskAttachmentService
import com.synchtask.task.application.service.TaskLinkService
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.application.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
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
    private val userService: UserService
) {

    // LINKS

    @PostMapping("/links")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun addLink(
        @PathVariable taskId: Long,
        @RequestParam title: String,
        @RequestParam url: String,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskLinkDTO> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found")

        val result = taskLinkService.addLink(taskId, title, url, actor)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/links")
    fun listLinks(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskLinkDTO>> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found")

        return ResponseEntity.ok(
            taskLinkService.listLinks(taskId, actor)
        )
    }

    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun deleteLink(
        @PathVariable taskId: Long,
        @PathVariable linkId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found")

        taskLinkService.removeLink(taskId, linkId, actor)
        return ResponseEntity.noContent().build()
    }

    // ATTACHMENTS

    @PostMapping("/attachments")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun uploadFile(
        @PathVariable taskId: Long,
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskAttachmentDTO> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found")

        val result = taskAttachmentService.uploadFile(taskId, file, actor)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/attachments")
    fun listAttachments(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskAttachmentDTO>> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found")

        return ResponseEntity.ok(
            taskAttachmentService.listAttachments(taskId, actor)
        )
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    fun deleteAttachment(
        @PathVariable taskId: Long,
        @PathVariable attachmentId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {

        val actor = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("User not found")

        taskAttachmentService.deleteAttachment(taskId, attachmentId, actor)
        return ResponseEntity.noContent().build()
    }
}
