package com.synchtask.repositories

import com.synchtask.entities.Task
import com.synchtask.entities.TaskAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * **Task Attachment Repository**
 *
 * Handles persistence of file attachments associated with tasks.
 */
@Repository
interface TaskAttachmentRepository : JpaRepository<TaskAttachment, Long> {
    fun findAllByTask(task: Task): List<TaskAttachment>
    fun deleteByTaskAndId(task: Task, attachmentId: Long): Int
}
