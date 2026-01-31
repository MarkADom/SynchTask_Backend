package com.synchtask.repositories

import com.synchtask.entities.Task
import com.synchtask.entities.TaskComment
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskCommentRepository : JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = ["user"])
    fun findByTaskOrderByCreatedAtAsc(task: Task): List<TaskComment>
}
