package com.synchtask.task.application.dto

import com.synchtask.task.domain.entity.TaskAttachment
import java.time.LocalDateTime

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
