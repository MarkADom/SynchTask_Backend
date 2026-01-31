package com.synchtask.repositories

import com.synchtask.entities.Board
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : JpaRepository<Board, Long> {

    fun findByOwner(user: User): List<Board>

    fun findByCollaboratorsContaining(user: User): List<Board>

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.collaborators WHERE b.id IN :ids")
    fun findAllWithCollaboratorsById(@Param("ids") ids: List<Long>): List<Board>
}
