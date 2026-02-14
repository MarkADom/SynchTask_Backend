package com.synchtask.board.application.service

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.application.dto.BoardCollaboratorUpdateDTO
import com.synchtask.board.application.dto.BoardCreateDTO
import com.synchtask.board.application.dto.BoardResponseDTO
import com.synchtask.board.application.dto.BoardSimpleDTO
import com.synchtask.board.application.dto.BoardUpdateDTO
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.board.presentation.mapper.BoardMapper
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository,
    private val activityService: ActivityService,
) {
    private val logger = LoggerFactory.getLogger(BoardService::class.java)

    @Transactional
    fun createBoard(dto: BoardCreateDTO, actor: User): BoardResponseDTO {
        val board =
            Board(
                name = dto.name,
                color = dto.color ?: "#605FA",
                description = dto.description ?: "",
                owner = actor
            )

        val saved = boardRepository.save(board)

        activityService.record(
            actor = actor,
            type = ActivityType.BOARD_CREATED,
            referenceId = saved.id,
            description = "Board '${saved.name}' criada"
        )

        logger.info("Board '${saved.name}' created by ${actor.email}")
        return BoardMapper.toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getBoardsForUser(actor: User): List<BoardResponseDTO> = boardRepository.findByOwner(actor)
        .map(BoardMapper::toResponse)

    @Transactional(readOnly = true)
    fun getBoardAccessibleByUser(id: Long, actor: User): BoardResponseDTO {
        val board =
            boardRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("Board not found: ID $id") }

        if (!board.hasAccess(actor)) {
            throw UnauthorizedAccessException("You do not have access to this board.")
        }

        return BoardMapper.toResponse(board)
    }

    @Transactional
    fun updateBoard(id: Long, dto: BoardUpdateDTO, actor: User): BoardResponseDTO {
        val board =
            boardRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("Board not found: ID $id") }

        if (!board.isOwnedBy(actor)) {
            throw UnauthorizedAccessException("Only the board owner can update it.")
        }

        board.updateFrom(dto)
        board.updatedAt = LocalDateTime.now()

        val updated = boardRepository.save(board)

        activityService.record(
            actor = actor,
            type = ActivityType.BOARD_UPDATED,
            referenceId = board.id,
            description = "Board '${board.name}' atualizada"
        )

        logger.info("Board ID $id updated by ${actor.email}")
        return BoardMapper.toResponse(updated)
    }

    @Transactional
    fun deleteBoard(boardId: Long, actor: User) {
        val board =
            boardRepository.findById(boardId)
                .orElseThrow { ResourceNotFoundException("Board not found with ID: $boardId") }

        if (!board.isOwnedBy(actor)) {
            throw UnauthorizedAccessException("You are not authorized to delete this board.")
        }

        val snapshot =
            ActivityContextSnapshot(
                ownerEmail = board.owner.email,
                collaboratorEmails = board.collaborators.map { it.email }.toSet()
            )

        boardRepository.delete(board)

        activityService.record(
            actor = actor,
            type = ActivityType.BOARD_DELETED,
            referenceId = board.id,
            description = "Board '${board.name}' removida",
            contextSnapshot = snapshot
        )

        logger.info("Board ID $boardId deleted by ${actor.email}")
    }

    @Transactional(readOnly = true)
    fun getBoardsSharedWithUser(actor: User): List<BoardResponseDTO> =
        boardRepository.findByCollaboratorsContaining(actor)
            .map(BoardMapper::toResponse)

    @Transactional(readOnly = true)
    fun getSimpleBoardsForUser(actor: User): List<BoardSimpleDTO> {
        val owned = boardRepository.findByOwner(actor)
        val shared = boardRepository.findByCollaboratorsContaining(actor)

        return (owned + shared)
            .distinctBy { it.id }
            .map {
                BoardSimpleDTO(
                    id = it.id ?: throw IllegalStateException("Board ID cannot be null"),
                    name = it.name
                )
            }
    }

    @Transactional
    fun updateCollaborators(boardId: Long, dto: BoardCollaboratorUpdateDTO, actor: User): BoardResponseDTO {
        val board =
            boardRepository.findById(boardId)
                .orElseThrow { ResourceNotFoundException("Board not found: ID $boardId") }

        if (!board.isOwnedBy(actor)) {
            throw UnauthorizedAccessException("Only the board owner can update collaborators.")
        }

        val collaborators = userRepository.findAllById(dto.userIds).toSet()

        if (collaborators.size != dto.userIds.size) {
            throw ResourceNotFoundException("Some users not found")
        }

        board.collaborators.clear()
        board.collaborators.addAll(collaborators)
        board.updatedAt = LocalDateTime.now()

        val updated = boardRepository.save(board)

        activityService.record(
            actor = actor,
            type = ActivityType.BOARD_COLLABORATORS_UPDATED,
            referenceId = board.id,
            description = "Colaboradores do board '${board.name}' atualizados"
        )

        logger.info("Board collaborators updated for board ID ${board.id} by ${actor.email}")
        return BoardMapper.toResponse(updated)
    }
}
