package com.synchtask.repositories

import com.synchtask.entities.Task
import com.synchtask.entities.TaskLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * **Task Link Repository**
 *
 * Handles persistence of links associated with tasks.
 */
@Repository
interface TaskLinkRepository : JpaRepository<TaskLink, Long> {
    fun findAllByTask(task: Task): List<TaskLink>
    fun deleteByTaskAndId(task: Task, linkId: Long): Int
}
