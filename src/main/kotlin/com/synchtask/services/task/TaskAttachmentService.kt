package com.synchtask.services.task

import com.synchtask.dtos.task.TaskAttachmentDTO
import com.synchtask.entities.TaskAttachment
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.repositories.TaskAttachmentRepository
import com.synchtask.repositories.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class TaskAttachmentService(
    private val taskRepository: TaskRepository,
    private val taskAttachmentRepository: TaskAttachmentRepository,
) {

    private val logger = LoggerFactory.getLogger(TaskAttachmentService::class.java)

    @Transactional
    fun uploadFile(taskId: Long, file: MultipartFile, userEmail: String): TaskAttachmentDTO {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (task.owner.email != userEmail) {
            throw UnauthorizedAccessException("You do not have permission to upload files to this task.")
        }

        val fakeUrl = "https://cdn.synchtask.app/files/${UUID.randomUUID()}/${file.originalFilename}"
        val attachment = taskAttachmentRepository.save(
            TaskAttachment(
                task = task,
                fileName = file.originalFilename ?: "unnamed-file",
                fileUrl = fakeUrl
            )
        )

        logger.info("User '$userEmail' uploaded file '${attachment.fileName}' to task '${task.title}'")
        return TaskAttachmentDTO.fromEntity(attachment)
    }

    fun listAttachments(taskId: Long, userEmail: String): List<TaskAttachmentDTO> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (task.owner.email != userEmail) {
            throw UnauthorizedAccessException("You do not have permission to view attachments for this task.")
        }

        return taskAttachmentRepository.findAllByTask(task).map { TaskAttachmentDTO.fromEntity(it) }
    }

    @Transactional
    fun deleteAttachment(taskId: Long, attachmentId: Long, userEmail: String): Boolean {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (task.owner.email != userEmail) {
            throw UnauthorizedAccessException("You do not have permission to delete attachments from this task.")
        }

        val deletedCount = taskAttachmentRepository.deleteByTaskAndId(task, attachmentId)
        if (deletedCount > 0) {
            logger.info("User '$userEmail' deleted attachment $attachmentId from task '${task.title}'")
            return true
        }

        logger.warn("Attachment $attachmentId not found or not associated with task $taskId")
        return false
    }
}
