package com.synchtask.task.application.service

import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole

internal fun hasTaskAccess(
    task: Task,
    actor: User,
    taskMemberRepository: TaskMemberRepository,
    boardMemberRepository: BoardMemberRepository,
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
    return false
}

internal fun resolveTaskMembershipUsers(
    task: Task,
    taskMemberRepository: TaskMemberRepository,
): Set<User> {
    val taskId = task.id ?: return emptySet()
    return taskMemberRepository.findAllByTaskId(taskId).map { it.user }.toSet()
}
