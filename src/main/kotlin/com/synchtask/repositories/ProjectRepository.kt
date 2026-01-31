package com.synchtask.repositories

import com.synchtask.entities.Project
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {

    fun findAllByOwner(owner: User): List<Project>
}
