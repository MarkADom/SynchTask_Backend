package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class BoardNotificationPolicy(
    private val boardRepository: BoardRepository
) : NotificationPolicy {

    override fun supports(activity: Activity): Boolean =
        activity.type in setOf(
            ActivityType.BOARD_CREATED,
            ActivityType.BOARD_UPDATED,
            ActivityType.BOARD_DELETED,
            ActivityType.BOARD_COLLABORATORS_UPDATED
        )

    override fun resolveRecipients(
        activity: Activity,
        contextSnapshot: ActivityContextSnapshot?
    ): Set<User> {
        if (activity.type == ActivityType.BOARD_DELETED) {
            val emails = buildSet {
                contextSnapshot?.ownerEmail?.let(::add)
                addAll(contextSnapshot?.collaboratorEmails ?: emptySet())
            }.filter { it != activity.actor.email }.toSet()

            // User to keep current contract
            return boardRepository.findAll().asSequence() // simple fallback
                .flatMap { sequenceOf(it.owner) + it.collaborators.asSequence() }
                .filter { it.email in emails }
                .toSet()
        }

        val boardId = activity.referenceId ?: return emptySet()
        val board = boardRepository.findById(boardId).orElse(null) ?: return emptySet()

        return when (activity.type) {
            ActivityType.BOARD_CREATED -> setOf(board.owner)

            ActivityType.BOARD_UPDATED,
            ActivityType.BOARD_COLLABORATORS_UPDATED ->
                (board.collaborators + board.owner)
                    .filter { it.id != activity.actor.id }
                    .toSet()

            ActivityType.BOARD_DELETED ->
                emptySet()

            else -> emptySet()
        }
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: "Board updated"

    override fun notificationType(): NotificationType =
        NotificationType.GROUP
}
