package com.synchtask.notification.application.policy

import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class ProjectNotificationPolicy(
    private val projectRepository: ProjectRepository
) : NotificationPolicy {

    override fun supports(activity: Activity): Boolean =
        activity.type in setOf(
            ActivityType.PROJECT_CREATED,
            ActivityType.PROJECT_UPDATED,
            ActivityType.PROJECT_DELETED
        )

    override fun resolveRecipients(activity: Activity): Set<User> {
        val projectId = activity.referenceId ?: return emptySet()
        val project = projectRepository.findById(projectId).orElse(null) ?: return emptySet()

        val boardUsers = project.boards
            .flatMap { it.collaborators + it.owner }

        return (boardUsers + project.owner)
            .filter { it.id != activity.actor.id }
            .toSet()
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: when (activity.type) {
            ActivityType.PROJECT_CREATED -> "New project created"
            ActivityType.PROJECT_UPDATED -> "Project updated"
            ActivityType.PROJECT_DELETED -> "Project deleted"
            else -> "Project activity"
        }

    override fun notificationType(): NotificationType =
        NotificationType.SYSTEM
}
