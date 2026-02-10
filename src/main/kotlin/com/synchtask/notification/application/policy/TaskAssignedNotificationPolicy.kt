package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class TaskAssignedNotificationPolicy(
    private val taskRepository: TaskRepository
) : NotificationPolicy {

    override fun supports(activity: Activity): Boolean =
        activity.type == ActivityType.TASK_ASSIGNED

    override fun resolveRecipients(
        activity: Activity,
        contextSnapshot: ActivityContextSnapshot?
    ): Set<User> {

        val taskId = activity.referenceId ?: return emptySet()
        val task = taskRepository.findById(taskId).orElse(null) ?: return emptySet()

        // Notifica apenas os novos colaboradores (exclui o actor)
        return task.collaborators
            .filter { it.id != activity.actor.id }
            .toSet()
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: "You were assigned to a task"

    override fun notificationType(): NotificationType =
        NotificationType.TASK_UPDATE
}
