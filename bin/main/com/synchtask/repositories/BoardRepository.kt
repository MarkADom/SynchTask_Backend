package com.synchtask.repositories

import com.synchtask.entities.Board
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for accessing Board data.
 */
@Repository
interface BoardRepository : JpaRepository<Board, Long> {

    /**
     * Returns all boards owned by the specified user.
     */
    fun findByOwner(user: User): List<Board>

    /**
     * Returns all boards where the specified user is listed as a collaborator.
     */
    fun findByCollaboratorsContaining(user: User): List<Board>

    /**
     * Returns boards with collaborators eagerly loaded by board IDs.
     */
    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.collaborators WHERE b.id IN :ids")
    fun findAllWithCollaboratorsById(@Param("ids") ids: List<Long>): List<Board>
}
