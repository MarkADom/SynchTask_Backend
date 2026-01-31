package com.synchtask.repositories

import com.synchtask.entities.Project
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository interface for CRUD operations on Project.
 */
@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    /** List all projects belonging to a given owner. */
    fun findAllByOwner(owner: User): List<Project>
}
