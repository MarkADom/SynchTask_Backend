package com.synchtask.task.domain.repository

import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskAttachmentRepository : JpaRepository<TaskAttachment, Long> {
    fun findAllByTask(task: Task): List<TaskAttachment>
    fun deleteByTaskAndId(task: Task, attachmentId: Long): Int
}
