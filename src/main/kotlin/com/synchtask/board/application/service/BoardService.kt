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
import com.synchtask.board.domain.entity.BoardMember
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.board.presentation.mapper.BoardMapper
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val boardMemberRepository: BoardMemberRepository,
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
        boardMemberRepository.save(
            BoardMember(
                board = saved,
                user = actor,
                role = MembershipRole.OWNER,
                createdByUser = actor
            )
        )

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
    fun getBoardsForUser(actor: User): List<BoardResponseDTO> {
        if (actor.role == UserRole.ADMIN) {
            return boardRepository.findAll().map(BoardMapper::toResponse)
        }

        val actorId = actor.id ?: return emptyList()
        val boardIds = boardMemberRepository.findAllByUserId(actorId).mapNotNull { it.board.id }
        if (boardIds.isEmpty()) return emptyList()

        return boardRepository.findAllById(boardIds).map(BoardMapper::toResponse)
    }

    @Transactional(readOnly = true)
    fun getBoardAccessibleByUser(id: Long, actor: User): BoardResponseDTO {
        val board =
            boardRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("Board not found: ID $id") }

        if (!hasBoardAccess(board, actor)) {
            throw UnauthorizedAccessException("You do not have access to this board.")
        }

        return BoardMapper.toResponse(board)
    }

    @Transactional
    fun updateBoard(id: Long, dto: BoardUpdateDTO, actor: User): BoardResponseDTO {
        val board =
            boardRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("Board not found: ID $id") }

        if (!isBoardOwner(board, actor)) {
            throw UnauthorizedAccessException("Only the board owner can update it.")
        }

        board.update(dto.name, dto.color, dto.description)

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

        if (!isBoardOwner(board, actor)) {
            throw UnauthorizedAccessException("You are not authorized to delete this board.")
        }

        val snapshot =
            ActivityContextSnapshot(
                ownerEmail = board.owner.email,
                collaboratorEmails = board.members.map { it.user.email }.toSet()
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
    fun getBoardsSharedWithUser(actor: User): List<BoardResponseDTO> {
        if (actor.role == UserRole.ADMIN) {
            return boardRepository.findAll().map(BoardMapper::toResponse)
        }

        val actorId = actor.id ?: return emptyList()
        val boardIds = boardMemberRepository.findAllByUserId(actorId).mapNotNull { it.board.id }
        if (boardIds.isEmpty()) return emptyList()

        return boardRepository.findAllById(boardIds)
            .map(BoardMapper::toResponse)
    }

    @Transactional(readOnly = true)
    fun getSimpleBoardsForUser(actor: User): List<BoardSimpleDTO> {
        val boards = if (actor.role == UserRole.ADMIN) {
            boardRepository.findAll()
        } else {
            val actorId = actor.id ?: return emptyList()
            val boardIds = boardMemberRepository.findAllByUserId(actorId).mapNotNull { it.board.id }
            if (boardIds.isEmpty()) return emptyList()
            boardRepository.findAllById(boardIds)
        }
        return boards
            .distinctBy { it.id }
            .map {
                BoardSimpleDTO(
                    id = checkNotNull(it.id) { "Board ID cannot be null" },
                    name = it.name
                )
            }
    }

    @Transactional
    fun updateCollaborators(boardId: Long, dto: BoardCollaboratorUpdateDTO, actor: User): BoardResponseDTO {
        val board =
            boardRepository.findById(boardId)
                .orElseThrow { ResourceNotFoundException("Board not found: ID $boardId") }

        if (!isBoardOwner(board, actor)) {
            throw UnauthorizedAccessException("Only the board owner can update collaborators.")
        }

        val collaborators = userRepository.findAllById(dto.userIds).toSet()
        if (collaborators.size != dto.userIds.size) {
            throw ResourceNotFoundException("Some users not found")
        }
        val memberships = boardMemberRepository.findAllByBoardId(boardId)
        val ownerMemberships = memberships.filter { it.role == MembershipRole.OWNER }
        boardMemberRepository.deleteAll(memberships.filter { it.role != MembershipRole.OWNER })

        val ownerIds = ownerMemberships.mapNotNull { it.user.id }.toSet()
        val newCollaboratorMemberships = collaborators
            .filter { collaborator -> collaborator.id !in ownerIds }
            .map { collaborator ->
                BoardMember(
                    board = board,
                    user = collaborator,
                    role = MembershipRole.COLLABORATOR,
                    createdByUser = actor
                )
            }
        boardMemberRepository.saveAll(newCollaboratorMemberships)

        board.updatedAt = LocalDateTime.now()
        val saved = boardRepository.save(board)

        activityService.record(
            actor = actor,
            type = ActivityType.BOARD_UPDATED,
            referenceId = saved.id,
            description = "Collaborators updated"
        )

        logger.info("Board collaborators updated for board ID ${board.id} by ${actor.email}")
        return BoardMapper.toResponse(saved)
    }

    private fun hasBoardAccess(board: Board, actor: User): Boolean {
        if (actor.role == UserRole.ADMIN) return true

        val boardId = board.id ?: return false
        val actorId = actor.id ?: return false

        return boardMemberRepository.existsByBoardIdAndUserId(boardId, actorId)
    }

    private fun isBoardOwner(board: Board, actor: User): Boolean {
        if (actor.role == UserRole.ADMIN) return true

        val boardId = board.id ?: return false
        val actorId = actor.id ?: return false
        val membership = boardMemberRepository.findByBoardIdAndUserId(boardId, actorId)

        return membership?.role == MembershipRole.OWNER
    }
}
