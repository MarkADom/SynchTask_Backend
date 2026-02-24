package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskAttachmentDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskAttachment
import com.synchtask.task.domain.repository.TaskAttachmentRepository
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.task.presentation.mapper.TaskMapper
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class TaskAttachmentService(
    private val taskRepository: TaskRepository,
    private val taskMemberRepository: TaskMemberRepository,
    private val boardMemberRepository: BoardMemberRepository,
    private val taskAttachmentRepository: TaskAttachmentRepository,
    private val activityService: ActivityService,
) {
    private val logger = LoggerFactory.getLogger(TaskAttachmentService::class.java)

    @Transactional
    fun uploadFile(taskId: Long, file: MultipartFile, user: User): TaskAttachmentDTO {
        val task =
            taskRepository.findById(taskId)
                .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException(
                "User ${user.email} is not allowed to upload attachments to this task."
            )
        }

        val fakeUrl =
            "https://cdn.synchtask.app/files/${UUID.randomUUID()}/${file.originalFilename}"

        val attachment =
            taskAttachmentRepository.save(
                TaskAttachment(
                    task = task,
                    fileName = file.originalFilename ?: "unnamed-file",
                    fileUrl = fakeUrl
                )
            )

        activityService.record(
            actor = user,
            type = ActivityType.TASK_ATTACHMENT_ADDED,
            referenceId = task.id,
            description = "File '${attachment.fileName}' added"
        )

        logger.info(
            "User '${user.email}' uploaded file '${attachment.fileName}' to task '${task.title}'"
        )

        return TaskMapper.toAttachmentDto(attachment)
    }

    @Transactional(readOnly = true)
    fun listAttachments(taskId: Long, user: User): List<TaskAttachmentDTO> {
        val task =
            taskRepository.findById(taskId)
                .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (!canAccessTask(task, user)) {
            throw UnauthorizedAccessException(
                "User ${user.email} is not allowed to view attachments for this task."
            )
        }

        return taskAttachmentRepository
            .findAllByTask(task)
            .map { TaskMapper.toAttachmentDto(it) }
    }

    @Transactional
    fun deleteAttachment(taskId: Long, attachmentId: Long, user: User) {
        val task =
            taskRepository.findById(taskId)
                .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (!canEditTask(task, user)) {
            throw UnauthorizedAccessException(
                "User ${user.email} is not allowed to delete attachments from this task."
            )
        }

        val deleted = taskAttachmentRepository.deleteByTaskAndId(task, attachmentId)

        if (deleted == 0) {
            throw ResourceNotFoundException(
                "Attachment $attachmentId not found for task $taskId"
            )
        }

        activityService.record(
            actor = user,
            type = ActivityType.TASK_ATTACHMENT_REMOVED,
            referenceId = task.id,
            description = "File removed"
        )

        logger.info(
            "User '${user.email}' deleted attachment $attachmentId from task '${task.title}'"
        )
    }

    private fun canEditTask(
        task: Task,
        actor: User,
    ): Boolean =
        hasTaskAccess(
            task,
            actor,
            taskMemberRepository,
            boardMemberRepository,
            logger,
            "task attachment"
        )

    private fun canAccessTask(
        task: Task,
        actor: User,
    ): Boolean =
        hasTaskAccess(
            task,
            actor,
            taskMemberRepository,
            boardMemberRepository,
            logger,
            "task attachment"
        )
}
