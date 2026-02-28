package com.synchtask.project.domain.repository

import com.synchtask.project.domain.entity.Project
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    // Performance rationale: list endpoints need project members and boards; eager graph avoids lazy-loading bursts.
    @EntityGraph(attributePaths = ["projectMembers.user", "boards"])
    override fun findAll(): List<Project>

    @EntityGraph(attributePaths = ["projectMembers.user", "boards"])
    override fun findAllById(ids: Iterable<Long>): List<Project>
}
