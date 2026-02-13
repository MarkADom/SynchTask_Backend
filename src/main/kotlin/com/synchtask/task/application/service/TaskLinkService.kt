package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.task.application.dto.TaskLinkDTO
import com.synchtask.task.domain.entity.TaskLink
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.domain.repository.TaskLinkRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.task.presentation.mapper.TaskMapper
import com.synchtask.user.domain.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskLinkService(
    private val taskRepository: TaskRepository,
    private val taskLinkRepository: TaskLinkRepository,
    private val activityService: ActivityService,
) {

    private val logger = LoggerFactory.getLogger(TaskLinkService::class.java)

    @Transactional
    fun addLink(
        taskId: Long,
        title: String,
        url: String,
        user: User
    ): TaskLinkDTO {

        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (!task.canBeEditedBy(user)) {
            throw UnauthorizedAccessException("User ${user.email} is not allowed to modify this task")
        }

        val link = taskLinkRepository.save(
            TaskLink(
                task = task,
                title = title,
                url = url
            )
        )

        activityService.record(
            actor = user,
            type = ActivityType.TASK_UPDATED,
            referenceId = task.id,
            description = "Adicionou link '${link.title}'"
        )

        logger.info("User '${user.email}' added link '${link.title}' to task '${task.title}'")

        return TaskMapper.toLinkDto(link)
    }

    @Transactional(readOnly = true)
    fun listLinks(
        taskId: Long,
        user: User
    ): List<TaskLinkDTO> {

        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (!task.canBeAccessedBy(user)) {
            throw UnauthorizedAccessException("User ${user.email} is not allowed to view links for this task")
        }

        return taskLinkRepository
            .findAllByTask(task)
            .map { TaskMapper.toLinkDto(it) }
    }

    @Transactional
    fun removeLink(
        taskId: Long,
        linkId: Long,
        user: User
    ) {

        val task = taskRepository.findById(taskId)
            .orElseThrow { ResourceNotFoundException("Task not found with ID $taskId") }

        if (!task.canBeEditedBy(user)) {
            throw UnauthorizedAccessException("User ${user.email} is not allowed to modify this task")
        }

        val deleted = taskLinkRepository.deleteByTaskAndId(task, linkId)

        if (deleted == 0) {
            throw ResourceNotFoundException("Link $linkId not found for task $taskId")
        }

        activityService.record(
            actor = user,
            type = ActivityType.TASK_UPDATED,
            referenceId = task.id,
            description = "Removeu um link da task"
        )

        logger.info("User '${user.email}' removed link $linkId from task '${task.title}'")
    }
}
