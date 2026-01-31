package com.synchtask.controllers

import com.synchtask.dtos.board.BoardCollaboratorUpdateDTO
import com.synchtask.dtos.board.BoardCreateDTO
import com.synchtask.dtos.board.BoardResponseDTO
import com.synchtask.dtos.board.BoardSimpleDTO
import com.synchtask.dtos.board.BoardUpdateDTO
import com.synchtask.repositories.BoardRepository
import com.synchtask.services.board.BoardService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * Board Controller
 *
 * REST API endpoints to manage Boards and Collaborators.
 */
@RestController
@RequestMapping("/boards")
@SecurityRequirement(name = "BearerAuth")
class BoardController(
    private val boardService: BoardService,
    private val boardRepository: BoardRepository
) {

    private val logger = LoggerFactory.getLogger(BoardController::class.java)

    /**
     * Create a new Board owned by the authenticated user.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createBoard(
        @RequestBody request: BoardCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<BoardResponseDTO> {
        val board = boardService.createBoard(request, user.username)
        return ResponseEntity.ok(board)
    }

    /**
     * Retrieve all boards owned by the current user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getBoards(
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<BoardResponseDTO>> {
        val boards = boardService.getBoardsForUser(user.username)
        return ResponseEntity.ok(boards)
    }

    /**
     * Retrieve a specific board by ID if the user has access (owner or collaborator).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getBoard(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<BoardResponseDTO> {
        val board = boardService.getBoardAccessibleByUser(id, user.username)
        return ResponseEntity.ok(board)
    }

    /**
     * Update a Board.
     * Only the board owner can perform this operation.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateBoard(
        @PathVariable id: Long,
        @RequestBody request: BoardUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<BoardResponseDTO> {
        val updated = boardService.updateBoard(id, request, user.username)
        return ResponseEntity.ok(updated)
    }

    /**
     * Delete a Board.
     * Only the board owner can delete their own boards.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun deleteBoard(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        boardService.deleteBoard(id, user.username)
        return ResponseEntity.ok("Board deleted successfully.")
    }

    /**
     * List all boards shared with the authenticated user (collaborations).
     */
    @GetMapping("/shared")
    @PreAuthorize("isAuthenticated()")
    fun getSharedBoards(
        @AuthenticationPrincipal user: UserDetails
    ): List<BoardResponseDTO> {
        return boardService.getBoardsSharedWithUser(user.username)
    }

    /**
     * Update board collaborators (only accessible by the board owner).
     */
    @PutMapping("/{id}/collaborators")
    @PreAuthorize("isAuthenticated()")
    fun updateBoardCollaborators(
        @PathVariable id: Long,
        @RequestBody dto: BoardCollaboratorUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<BoardResponseDTO> {
        val updated = boardService.updateCollaborators(id, dto, user.username)
        return ResponseEntity.ok(updated)
    }

    @GetMapping("/simple")
    @PreAuthorize("isAuthenticated()")
    fun getSimpleBoards(): List<BoardSimpleDTO> {
        return boardRepository.findAll().map { BoardSimpleDTO(it.id!!, it.name) }
    }
}
