package com.synchtask.services.task

import com.synchtask.dtos.task.TaskLinkDTO
import com.synchtask.entities.TaskLink
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.repositories.TaskLinkRepository
import com.synchtask.repositories.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskLinkService(
    private val taskRepository: TaskRepository,
    private val taskLinkRepository: TaskLinkRepository
) {

    private val logger = LoggerFactory.getLogger(TaskLinkService::class.java)

    @Transactional
    fun addLink(taskId: Long, title: String, url: String, userEmail: String): TaskLinkDTO {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (task.owner.email != userEmail) {
            logger.warn("Unauthorized attempt to add link to task $taskId by $userEmail")
            throw UnauthorizedAccessException("You do not have permission to modify this task")
        }

        val link = taskLinkRepository.save(TaskLink(task = task, title = title, url = url))
        logger.info("Added link '$title' to task '${task.title}' by $userEmail")

        return TaskLinkDTO.fromEntity(link)
    }

    fun listLinks(taskId: Long): List<TaskLinkDTO> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        return taskLinkRepository.findAllByTask(task).map { TaskLinkDTO.fromEntity(it) }
    }

    @Transactional
    fun removeLink(taskId: Long, linkId: Long, userEmail: String): Boolean {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (task.owner.email != userEmail) {
            logger.warn("Unauthorized attempt to delete link $linkId from task $taskId by $userEmail")
            throw UnauthorizedAccessException("You do not have permission to modify this task")
        }

        val deletedCount = taskLinkRepository.deleteByTaskAndId(task, linkId)
        if (deletedCount > 0) {
            logger.info("Removed link $linkId from task '${task.title}' by $userEmail")
            return true
        }

        logger.warn("Link $linkId not found for task $taskId or already deleted")
        return false
    }
}
