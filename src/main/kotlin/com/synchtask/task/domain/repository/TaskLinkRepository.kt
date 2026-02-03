package com.synchtask.task.domain.repository

import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskLinkRepository : JpaRepository<TaskLink, Long> {
    fun findAllByTask(task: Task): List<TaskLink>
    fun deleteByTaskAndId(task: Task, linkId: Long): Int
}
