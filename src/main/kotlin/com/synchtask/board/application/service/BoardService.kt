package com.synchtask.board.application.service

import com.synchtask.board.application.dto.BoardCollaboratorUpdateDTO
import com.synchtask.board.application.dto.BoardCreateDTO
import com.synchtask.board.application.dto.BoardResponseDTO
import com.synchtask.board.application.dto.BoardUpdateDTO
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.board.presentation.mapper.BoardMapper
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(BoardService::class.java)

    @Transactional
    fun createBoard(dto: BoardCreateDTO, userEmail: String): BoardResponseDTO {
        val user = getUser(userEmail)
        val board = Board(
            name = dto.name,
            color = dto.color,
            description = dto.description,
            owner = user
        )
        val saved = boardRepository.save(board)
        logger.info("Board '${dto.name}' created by $userEmail")
        return BoardMapper.toResponse(saved)
    }

    fun getBoardsForUser(userEmail: String): List<BoardResponseDTO> {
        val user = getUser(userEmail)
        return boardRepository.findByOwner(user).map(BoardMapper::toResponse)
    }

    fun getBoardAccessibleByUser(id: Long, userEmail: String): BoardResponseDTO {
        val user = getUser(userEmail)
        val board = boardRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Board not found: ID $id") }

        if (!board.hasAccess(user)) {
            throw UnauthorizedAccessException("You do not have access to this board.")
        }

        return BoardMapper.toResponse(board)
    }

    @Transactional
    fun updateBoard(id: Long, dto: BoardUpdateDTO, userEmail: String): BoardResponseDTO {
        val board = boardRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Board not found: ID $id") }

        if (!board.isOwnedBy(getUser(userEmail))) {
            throw UnauthorizedAccessException("Only the board owner can update it.")
        }

        board.name = dto.name
        board.color = dto.color
        board.description = dto.description
        board.updatedAt = LocalDateTime.now()

        val updated = boardRepository.save(board)
        logger.info("Board ID $id updated by $userEmail")
        return BoardMapper.toResponse(updated)
    }

    @Transactional
    fun deleteBoard(boardId: Long, userEmail: String) {
        val board = boardRepository.findById(boardId)
            .orElseThrow { ResourceNotFoundException("Board not found with ID: $boardId") }

        if (!board.isOwnedBy(getUser(userEmail))) {
            throw UnauthorizedAccessException("You are not authorized to delete this board.")
        }

        boardRepository.delete(board)
        logger.info("Board ID $boardId deleted by $userEmail")
    }

    fun getBoardsSharedWithUser(email: String): List<BoardResponseDTO> {
        val user = getUser(email)
        return boardRepository.findByCollaboratorsContaining(user)
            .map(BoardMapper::toResponse)
    }

    @Transactional
    fun updateCollaborators(boardId: Long, dto: BoardCollaboratorUpdateDTO, userEmail: String): BoardResponseDTO {
        val board = boardRepository.findById(boardId)
            .orElseThrow { ResourceNotFoundException("Board not found: ID $boardId") }

        if (!board.isOwnedBy(getUser(userEmail))) {
            throw UnauthorizedAccessException("Only the board owner can update collaborators.")
        }

        val collaborators = userRepository.findAllById(dto.userIds).toMutableSet()
        board.collaborators.clear()
        board.collaborators.addAll(collaborators)
        board.updatedAt = LocalDateTime.now()

        return BoardMapper.toResponse(boardRepository.save(board))
    }

    private fun getUser(email: String) =
        userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("User not found: $email") }
}
