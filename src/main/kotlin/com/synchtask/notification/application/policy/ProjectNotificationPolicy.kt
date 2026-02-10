package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.project.domain.repository.ProjectRepository
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

    override fun resolveRecipients(
        activity: Activity,
        contextSnapshot: ActivityContextSnapshot?
    ): Set<String> {
        if (activity.type == ActivityType.PROJECT_DELETED) {
            return buildSet {
                contextSnapshot?.ownerEmail?.let(::add)
                addAll(contextSnapshot?.memberEmails ?: emptySet())
            }.filter { it != activity.actor.email }.toSet()
        }

        val projectId = activity.referenceId ?: return emptySet()
        val project = projectRepository.findById(projectId).orElse(null) ?: return emptySet()

        return when (activity.type) {
            ActivityType.PROJECT_CREATED -> setOf(project.owner.email)

            ActivityType.PROJECT_UPDATED ->
                (project.members.map { it.email } + project.owner.email)
                    .filter { it != activity.actor.email }
                    .toSet()

            ActivityType.PROJECT_DELETED -> emptySet()
            else -> emptySet()
        }
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: "Project updated"

    override fun notificationType(): NotificationType =
        NotificationType.GROUP
}
