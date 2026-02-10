package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.task.domain.repository.TaskRepository
import org.springframework.stereotype.Component

@Component
class TaskCommentNotificationPolicy(
    private val taskRepository: TaskRepository
) : NotificationPolicy {

    override fun supports(activity: Activity): Boolean =
        activity.type == ActivityType.TASK_COMMENTED

    override fun resolveRecipients(
        activity: Activity,
        contextSnapshot: ActivityContextSnapshot?
    ): Set<String> {
        val taskId = activity.referenceId ?: return emptySet()
        val task = taskRepository.findById(taskId).orElse(null) ?: return emptySet()

        return (task.collaborators.map { it.email } + task.owner.email)
            .filter { it != activity.actor.email }
            .toSet()
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: "New comment on a task"

    override fun notificationType(): NotificationType =
        NotificationType.TASK_UPDATE
}
