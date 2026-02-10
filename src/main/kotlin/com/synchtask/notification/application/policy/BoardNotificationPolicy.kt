package com.synchtask.notification.application.policy

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
            ActivityType.BOARD_COLLABORATORS_UPDATED
        )

    override fun resolveRecipients(activity: Activity): Set<User> {
        val boardId = activity.referenceId ?: return emptySet()
        val board = boardRepository.findById(boardId).orElse(null) ?: return emptySet()

        return (board.collaborators + board.owner)
            .filter { it.id != activity.actor.id }
            .toSet()
    }

    override fun buildMessage(activity: Activity): String =
        activity.description ?: when (activity.type) {
            ActivityType.BOARD_CREATED -> "New board created"
            ActivityType.BOARD_UPDATED -> "Board updated"
            ActivityType.BOARD_COLLABORATORS_UPDATED -> "Board collaborators updated"
            else -> "Board activity"
        }

    override fun notificationType(): NotificationType =
        NotificationType.GROUP
}
