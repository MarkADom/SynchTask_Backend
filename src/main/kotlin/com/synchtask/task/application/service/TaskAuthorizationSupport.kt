package com.synchtask.task.application.service

import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.slf4j.Logger

internal fun hasTaskAccess(
    task: Task,
    actor: User,
    taskMemberRepository: TaskMemberRepository,
    boardMemberRepository: BoardMemberRepository,
    logger: Logger,
    source: String,
): Boolean {
    if (actor.role == UserRole.ADMIN) return true

    val taskId = task.id ?: return false
    val actorId = actor.id ?: return false
    if (taskMemberRepository.existsByTaskIdAndUserId(taskId, actorId)) {
        return true
    }

    val boardId = task.board.id
    if (boardId != null && boardMemberRepository.existsByBoardIdAndUserId(boardId, actorId)) {
        return true
    }

    // TODO: Remove legacy task/board fallback once membership migration is complete.
    val fallback =
        task.owner.id == actorId ||
            task.collaborators.any { it.id == actorId } ||
            task.board.owner.id == actorId ||
            task.board.collaborators.any { it.id == actorId }

    if (fallback) {
        logger.warn("Using {} legacy fallback access check for taskId={} userId={}", source, taskId, actorId)
    }
    return fallback
}

internal fun resolveTaskMembershipUsers(
    task: Task,
    taskMemberRepository: TaskMemberRepository,
    logger: Logger,
    source: String,
): Set<User> {
    val taskId = task.id ?: return task.collaborators
    val membershipUsers = taskMemberRepository.findAllByTaskId(taskId).map { it.user }.toSet()
    if (membershipUsers.isNotEmpty()) {
        return membershipUsers
    }

    // TODO: Remove legacy collaborator fallback once membership migration is complete.
    if (task.collaborators.isNotEmpty()) {
        logger.warn("Using {} legacy fallback recipients for taskId={}", source, taskId)
    }

    return task.collaborators
}
