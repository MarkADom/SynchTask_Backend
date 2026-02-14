package com.synchtask.task.application.service

import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.query.TaskSpecificationQueryBuilder
import com.synchtask.user.domain.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TaskSpecificationService(
    private val queryBuilder: TaskSpecificationQueryBuilder
) {
    fun findTasksByFilters(
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?,
        pageable: Pageable
    ): Page<Task> {
        return queryBuilder.execute(user, status, label, assigneeId, boardId, pageable)
    }
}
