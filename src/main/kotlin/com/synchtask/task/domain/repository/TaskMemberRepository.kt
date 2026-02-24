package com.synchtask.task.domain.repository

import com.synchtask.task.domain.entity.TaskMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskMemberRepository : JpaRepository<TaskMember, Long> {
    fun existsByTaskIdAndUserId(taskId: Long, userId: Long): Boolean

    fun findByTaskIdAndUserId(taskId: Long, userId: Long): TaskMember?

    fun findAllByTaskId(taskId: Long): List<TaskMember>
}
