package com.synchtask.repositories

import com.synchtask.entities.Task
import com.synchtask.entities.TaskComment
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * **Task Comment Repository**
 *
 * Handles database operations for task comments.
 */
@Repository
interface TaskCommentRepository : JpaRepository<TaskComment, Long> {

    /**
     * **Finds all comments for a task, ordered by creation date.**
     */
    @EntityGraph(attributePaths = ["user"])
    fun findByTaskOrderByCreatedAtAsc(task: Task): List<TaskComment>
}
