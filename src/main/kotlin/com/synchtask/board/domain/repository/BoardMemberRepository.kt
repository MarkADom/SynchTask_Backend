package com.synchtask.board.domain.repository

import com.synchtask.board.domain.entity.BoardMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardMemberRepository : JpaRepository<BoardMember, Long> {
    fun existsByBoardIdAndUserId(boardId: Long, userId: Long): Boolean

    fun findByBoardIdAndUserId(boardId: Long, userId: Long): BoardMember?

    fun findAllByBoardId(boardId: Long): List<BoardMember>

    fun findAllByUserId(userId: Long): List<BoardMember>
}
