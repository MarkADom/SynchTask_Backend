package com.synchtask.dtos.task

import com.synchtask.entities.TaskAttachment
import java.time.LocalDateTime

/**
 * DTO representing a file attached to a task.
 */
data class TaskAttachmentDTO(
    val id: Long?,
    val fileName: String,
    val fileUrl: String,
    val uploadedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(attachment: TaskAttachment): TaskAttachmentDTO {
            return TaskAttachmentDTO(
                id = attachment.id,
                fileName = attachment.fileName,
                fileUrl = attachment.fileUrl,
                uploadedAt = attachment.uploadedAt
            )
        }
    }
}
