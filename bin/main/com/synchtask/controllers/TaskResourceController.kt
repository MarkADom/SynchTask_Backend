package com.synchtask.controllers

import com.synchtask.dtos.task.TaskAttachmentDTO
import com.synchtask.dtos.task.TaskLinkDTO
import com.synchtask.services.task.TaskAttachmentService
import com.synchtask.services.task.TaskLinkService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * **Task Resource Controller**
 *
 * REST API for managing task links and file attachments.
 * Only users with proper roles (OWNER, ADMIN) can modify resources.
 */
@RestController
@RequestMapping("/tasks/{taskId}")
class TaskResourceController(
    private val taskLinkService: TaskLinkService,
    private val taskAttachmentService: TaskAttachmentService
) {

    // ------------------ LINKS ------------------

    /**
     * Adds a new link to the task.
     * Requires OWNER or ADMIN authority.
     */
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

    /**
     * Lists all links associated with a task.
     * Authenticated users can view them.
     */
    @GetMapping("/links")
    fun listLinks(@PathVariable taskId: Long): ResponseEntity<List<TaskLinkDTO>> {
        return ResponseEntity.ok(taskLinkService.listLinks(taskId))
    }

    /**
     * Deletes a specific link from the task.
     * Requires OWNER or ADMIN authority.
     */
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

    // ------------------ ATTACHMENTS ------------------

    /**
     * Uploads a file to the task.
     * Requires OWNER or ADMIN authority.
     */
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

    /**
     * Lists all attachments for a task.
     * Accessible by authenticated users.
     */
    @GetMapping("/attachments")
    fun listAttachments(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskAttachmentDTO>> {
        val files = taskAttachmentService.listAttachments(taskId, user.username)
        return ResponseEntity.ok(files)
    }

    /**
     * Deletes a specific attachment from the task.
     * Requires OWNER or ADMIN authority.
     */
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
