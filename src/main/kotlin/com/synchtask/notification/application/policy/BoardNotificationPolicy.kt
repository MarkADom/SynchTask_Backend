package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.notification.domain.entity.NotificationType
import org.springframework.stereotype.Component

@Component
class BoardNotificationPolicy(
    private val boardRepository: BoardRepository
) : NotificationPolicy {
    override fun supports(activity: Activity): Boolean = activity.type in
        setOf(
            ActivityType.BOARD_CREATED,
            ActivityType.BOARD_UPDATED,
            ActivityType.BOARD_DELETED,
            ActivityType.BOARD_COLLABORATORS_UPDATED
        )

    override fun resolveRecipients(activity: Activity, contextSnapshot: ActivityContextSnapshot?): Set<String> {
        if (activity.type == ActivityType.BOARD_DELETED) {
            return buildSet {
                contextSnapshot?.ownerEmail?.let(::add)
                addAll(contextSnapshot?.collaboratorEmails ?: emptySet())
            }.filter { it != activity.actor.email }.toSet()
        }

        val boardId = activity.referenceId ?: return emptySet()
        val board = boardRepository.findById(boardId).orElse(null) ?: return emptySet()

        return when (activity.type) {
            ActivityType.BOARD_CREATED -> setOf(board.owner.email)

            ActivityType.BOARD_UPDATED,
            ActivityType.BOARD_COLLABORATORS_UPDATED ->
                (board.members.map { it.user.email } + board.owner.email)
                    .filter { it != activity.actor.email }
                    .toSet()

            ActivityType.BOARD_DELETED -> emptySet()
            else -> emptySet()
        }
    }

    override fun buildMessage(activity: Activity): String = activity.description ?: "Board updated"

    override fun notificationType(): NotificationType = NotificationType.GROUP
}
