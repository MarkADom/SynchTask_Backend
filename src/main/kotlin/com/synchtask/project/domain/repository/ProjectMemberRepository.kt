package com.synchtask.project.domain.repository

import com.synchtask.project.domain.entity.ProjectMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectMemberRepository : JpaRepository<ProjectMember, Long> {
    fun existsByProjectIdAndUserId(projectId: Long, userId: Long): Boolean

    fun findByProjectIdAndUserId(projectId: Long, userId: Long): ProjectMember?

    fun findAllByProjectId(projectId: Long): List<ProjectMember>
}
