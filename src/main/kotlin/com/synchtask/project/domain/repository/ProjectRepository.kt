package com.synchtask.project.domain.repository

import com.synchtask.project.domain.entity.Project
import com.synchtask.user.domain.entity.User
import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    @Query(
        """
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.members
        LEFT JOIN FETCH p.boards
        WHERE p.owner = :owner
        """
    )
    fun findAllByOwnerWithMembersAndBoards(@Param("owner") owner: User): List<Project>
}
