package com.synchtask.task.domain.repository

import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskCommentRepository : JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = ["user"])
    fun findByTaskOrderByCreatedAtAsc(task: Task): List<TaskComment>

}
