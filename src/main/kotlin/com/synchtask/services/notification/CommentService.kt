package com.synchtask.services.notification

import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val taskCommentRepository: TaskCommentRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(CommentService::class.java)

    @Transactional
    fun addComment(taskId: Long, authorEmail: String, content: String): TaskComment {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found") }

        val user = userRepository.findByEmail(authorEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $authorEmail") }

        val comment = TaskComment(task = task, user = user, content = content)
        logger.info("Comment added to task ${task.title} by ${user.email}")

        return taskCommentRepository.save(comment)
    }
}
