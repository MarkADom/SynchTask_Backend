package com.synchtask.board.domain.repository

import com.synchtask.board.domain.entity.Board
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : JpaRepository<Board, Long> {
    // Performance rationale: list endpoints frequently access owner data during DTO mapping.
    @EntityGraph(attributePaths = ["owner"])
    override fun findAll(): List<Board>

    @EntityGraph(attributePaths = ["owner"])
    override fun findAllById(ids: Iterable<Long>): List<Board>
}
