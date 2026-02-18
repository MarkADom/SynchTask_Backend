package com.synchtask.board.domain.repository

import com.synchtask.board.domain.entity.Board
import com.synchtask.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : JpaRepository<Board, Long> {

    @Query(
        """
        SELECT b
        FROM Board b
        JOIN FETCH b.owner
        WHERE b.owner = :user
        """
    )
    fun findByOwnerWithOwnerFetched(@Param("user") user: User): List<Board>

    @Query(
        """
        SELECT DISTINCT b
        FROM Board b
        JOIN FETCH b.owner
        JOIN b.collaborators c
        WHERE c = :user
        """
    )
    fun findByCollaboratorsContainingWithOwnerFetched(@Param("user") user: User): List<Board>

    @Query(
        """
            SELECT b 
            FROM Board b 
            LEFT JOIN 
            FETCH b.collaborators 
            WHERE b.id 
            IN :ids"""
    )
    fun findAllWithCollaboratorsById(@Param("ids") ids: List<Long>): List<Board>
}
