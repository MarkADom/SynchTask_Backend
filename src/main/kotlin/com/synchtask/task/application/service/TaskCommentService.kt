package com.synchtask.task.application.service

import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.entities.NotificationType
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.repositories.UserRepository
import com.synchtask.services.notification.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskCommentService(
    private val taskCommentRepository: TaskCommentRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(TaskCommentService::class.java)

    @Transactional
    fun addComment(taskId: Long, userEmail: String, request: TaskCommentCreateDTO): TaskCommentResponseDTO {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found") }

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found") }

        if (user != task.owner && !task.collaborators.contains(user)) {
            throw IllegalAccessException("User not authorized to comment on this task")
        }

        val comment = TaskComment(
            task = task,
            user = user,
            content = request.content
        )

        taskCommentRepository.save(comment)

        (task.collaborators + task.owner).forEach {
            notificationService.sendNotification(
                userEmail = it.email,
                message = "New comment on task '${task.title}' by ${user.email}: ${request.content}",
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }

        val commentDTO = TaskCommentResponseDTO.fromEntity(comment)
        messagingTemplate.convertAndSend("/topic/tasks/${task.id}/comments", commentDTO)

        logger.info("Comment added by ${user.email} on task '${task.title}'")
        return commentDTO
    }

    fun getCommentsForTask(taskId: Long): List<TaskCommentResponseDTO> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found") }

        return taskCommentRepository.findByTaskOrderByCreatedAtAsc(task)
            .map { TaskCommentResponseDTO.fromEntity(it) }
    }
}
