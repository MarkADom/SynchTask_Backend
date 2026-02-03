package com.synchtask.board.presentation.controller

import com.synchtask.board.application.dto.BoardCollaboratorUpdateDTO
import com.synchtask.board.application.dto.BoardCreateDTO
import com.synchtask.board.application.dto.BoardResponseDTO
import com.synchtask.board.application.dto.BoardSimpleDTO
import com.synchtask.board.application.dto.BoardUpdateDTO
import com.synchtask.board.application.service.BoardService
import com.synchtask.board.domain.repository.BoardRepository
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Board-related HTTP endpoints.
 *
 * Ownership and collaborator rules are enforced at service level.
 */
@RestController
@RequestMapping("/boards")
@SecurityRequirement(name = "BearerAuth")
class BoardController(
    private val boardService: BoardService,
    private val boardRepository: BoardRepository
) {

    private val logger = LoggerFactory.getLogger(BoardController::class.java)

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createBoard(
        @RequestBody request: BoardCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<BoardResponseDTO> {
        val board = boardService.createBoard(request, user.username)
        return ResponseEntity.ok(board)
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getBoards(
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<BoardResponseDTO>> {
        val boards = boardService.getBoardsForUser(user.username)
        return ResponseEntity.ok(boards)
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getBoard(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<BoardResponseDTO> {
        val board = boardService.getBoardAccessibleByUser(id, user.username)
        return ResponseEntity.ok(board)
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun deleteBoard(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        boardService.deleteBoard(id, user.username)
        return ResponseEntity.ok("Board deleted successfully.")
    }

    @GetMapping("/shared")
    @PreAuthorize("isAuthenticated()")
    fun getSharedBoards(
        @AuthenticationPrincipal user: UserDetails
    ): List<BoardResponseDTO> {
        return boardService.getBoardsSharedWithUser(user.username)
    }

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
