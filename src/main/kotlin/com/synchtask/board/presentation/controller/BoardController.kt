package com.synchtask.board.presentation.controller

import com.synchtask.board.application.dto.BoardCollaboratorUpdateDTO
import com.synchtask.board.application.dto.BoardCreateDTO
import com.synchtask.board.application.dto.BoardResponseDTO
import com.synchtask.board.application.dto.BoardSimpleDTO
import com.synchtask.board.application.dto.BoardUpdateDTO
import com.synchtask.board.application.service.BoardService
import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.user.application.service.AuthenticatedUserService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
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
    private val authenticatedUserService: AuthenticatedUserService,
) {
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createBoard(
        @Valid @RequestBody request: BoardCreateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<BoardResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        val board = boardService.createBoard(request, actor)
        return ResponseEntity.ok(board)
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getBoards(@AuthenticationPrincipal user: UserDetails): ResponseEntity<List<BoardResponseDTO>> {
        val actor = authenticatedUserService.requireUser(user)

        val boards = boardService.getBoardsForUser(actor)
        return ResponseEntity.ok(boards)
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getBoard(@PathVariable id: Long, @AuthenticationPrincipal user: UserDetails): ResponseEntity<BoardResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        val board = boardService.getBoardAccessibleByUser(id, actor)
        return ResponseEntity.ok(board)
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateBoard(
        @PathVariable id: Long,
        @Valid @RequestBody request: BoardUpdateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<BoardResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        val updated = boardService.updateBoard(id, request, actor)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun deleteBoard(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        boardService.deleteBoard(id, actor)
        return ResponseEntity.ok(ApiMessageResponseDTO("Board deleted successfully."))
    }

    @GetMapping("/shared")
    @PreAuthorize("isAuthenticated()")
    fun getSharedBoards(@AuthenticationPrincipal user: UserDetails): List<BoardResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        return boardService.getBoardsSharedWithUser(actor)
    }

    @PutMapping("/{id}/collaborators")
    @PreAuthorize("isAuthenticated()")
    fun updateBoardCollaborators(
        @PathVariable id: Long,
        @Valid @RequestBody dto: BoardCollaboratorUpdateDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<BoardResponseDTO> {
        val actor = authenticatedUserService.requireUser(user)

        val updated = boardService.updateCollaborators(id, dto, actor)
        return ResponseEntity.ok(updated)
    }

    @GetMapping("/simple")
    @PreAuthorize("isAuthenticated()")
    fun getSimpleBoards(@AuthenticationPrincipal user: UserDetails): List<BoardSimpleDTO> {
        val actor = authenticatedUserService.requireUser(user)
        return boardService.getSimpleBoardsForUser(actor)
    }
}
