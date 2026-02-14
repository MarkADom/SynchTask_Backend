package com.synchtask.project.domain.repository

import com.synchtask.project.domain.entity.Project
import com.synchtask.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    fun findAllByOwner(owner: User): List<Project>
}
