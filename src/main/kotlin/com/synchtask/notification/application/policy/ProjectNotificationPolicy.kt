package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
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

    override fun resolveRecipients(
        activity: Activity,
        contextSnapshot: ActivityContextSnapshot?
    ): Set<User> {
        if (activity.type == ActivityType.PROJECT_DELETED) {
            val emails = buildSet {
                contextSnapshot?.ownerEmail?.let(::add)
                addAll(contextSnapshot?.memberEmails ?: emptySet())
            }.filter { it != activity.actor.email }.toSet()

            // fallback de transição (ver nota do BoardPolicy)
            return projectRepository.findAll().asSequence()
                .flatMap { sequenceOf(it.owner) + it.members.asSequence() }
                .filter { it.email in emails }
                .toSet()
        }

        val projectId = activity.referenceId ?: return emptySet()
        val project = projectRepository.findById(projectId).orElse(null) ?: return emptySet()

        return when (activity.type) {
            ActivityType.PROJECT_CREATED ->
                setOf(project.owner)

            ActivityType.PROJECT_UPDATED ->
                (project.members + project.owner)
                    .filter { it.id != activity.actor.id }
                    .toSet()

            ActivityType.PROJECT_DELETED ->
                emptySet()

            else -> emptySet()
        }
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: "Project updated"

    override fun notificationType(): NotificationType =
        NotificationType.GROUP
}
