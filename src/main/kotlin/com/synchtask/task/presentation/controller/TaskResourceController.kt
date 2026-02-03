package com.synchtask.task.presentation.controller

import com.synchtask.task.application.dto.TaskAttachmentDTO
import com.synchtask.task.application.dto.TaskLinkDTO
import com.synchtask.task.application.service.TaskAttachmentService
import com.synchtask.task.application.service.TaskLinkService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * Task-related resources (links and attachments).
 *
 * Permission and ownership checks are handled in the service layer.
 */
@RestController
@RequestMapping("/tasks/{taskId}")
class TaskResourceController(
    private val taskLinkService: TaskLinkService,
    private val taskAttachmentService: TaskAttachmentService
) {

    @PostMapping("/links")
    @PreAuthorize("hasAuthority('OWNER') or hasAuthority('ADMIN')")
    fun addLink(
        @PathVariable taskId: Long,
        @RequestParam title: String,
        @RequestParam url: String,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskLinkDTO> {
        val result = taskLinkService.addLink(taskId, title, url, user.username)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/links")
    fun listLinks(@PathVariable taskId: Long): ResponseEntity<List<TaskLinkDTO>> {
        return ResponseEntity.ok(taskLinkService.listLinks(taskId))
    }

    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('OWNER') or hasAuthority('ADMIN')")
    fun deleteLink(
        @PathVariable taskId: Long,
        @PathVariable linkId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        val deleted = taskLinkService.removeLink(taskId, linkId, user.username)
        return if (deleted) ResponseEntity.ok("Link deleted successfully")
        else ResponseEntity.notFound().build()
    }

    @PostMapping("/attachments")
    @PreAuthorize("hasAuthority('OWNER') or hasAuthority('ADMIN')")
    fun uploadFile(
        @PathVariable taskId: Long,
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskAttachmentDTO> {
        val result = taskAttachmentService.uploadFile(taskId, file, user.username)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/attachments")
    fun listAttachments(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskAttachmentDTO>> {
        val files = taskAttachmentService.listAttachments(taskId, user.username)
        return ResponseEntity.ok(files)
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('OWNER') or hasAuthority('ADMIN')")
    fun deleteAttachment(
        @PathVariable taskId: Long,
        @PathVariable attachmentId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        val deleted = taskAttachmentService.deleteAttachment(taskId, attachmentId, user.username)
        return if (deleted) ResponseEntity.ok("Attachment deleted successfully")
        else ResponseEntity.notFound().build()
    }
}
