package com.synchtask.repositories

import com.synchtask.entities.Task
import com.synchtask.entities.TaskStatus
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * **Task Repository**
 *
 * Handles database operations for tasks.
 */
@Repository
interface TaskRepository : JpaRepository<Task, Long> {

    /**
     * **Find tasks where the user is either the owner or a collaborator.**
     *
     * Uses `@EntityGraph` to fetch `collaborators` and `comments` in a single query.
     */
    @EntityGraph(attributePaths = ["collaborators", "comments"])
    @Query(
        """
        SELECT DISTINCT t FROM Task t 
        LEFT JOIN FETCH t.collaborators 
        LEFT JOIN FETCH t.comments 
        WHERE t.owner = :user OR :user MEMBER OF t.collaborators
    """
    )
    fun findTasksByUser(@Param("user") user: User): List<Task>

    /**
     * **Find tasks by status.**
     *
     * Uses `@EntityGraph` to avoid lazy loading issues.
     */
    @EntityGraph(attributePaths = ["owner", "collaborators"])
    fun findByStatus(status: TaskStatus): List<Task>

    /**
     * **Find tasks owned by a user.**
     */
    @EntityGraph(attributePaths = ["collaborators", "comments"])
    fun findByOwner(owner: User): List<Task>
}
