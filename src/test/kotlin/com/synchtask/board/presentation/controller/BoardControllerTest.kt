package com.synchtask.board.presentation.controller

import com.synchtask.board.application.dto.*
import com.synchtask.board.application.service.BoardService
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.User as SpringUser
import java.time.LocalDateTime
import kotlin.test.assertEquals

class BoardControllerTest {

    private lateinit var boardService: BoardService
    private lateinit var boardRepository: BoardRepository
    private lateinit var userService: UserService
    private lateinit var controller: BoardController

    private lateinit var userEntity: User
    private lateinit var userDetails: UserDetails

    @BeforeEach
    fun setup() {
        boardService = mockk()
        boardRepository = mockk()
        controller = BoardController(boardService, boardRepository, userService)

        userEntity = User(
            id = 1L,
            name = "User",
            email = "user@test.com",
            passwordHash = "hash",
            role = UserRole.USER
        )

        userDetails = SpringUser(
            userEntity.email,
            "hash",
            emptyList()
        )
        every { userService.getUserByEmail(userEntity.email) } returns userEntity
    }




    private fun newBoard(id: Long = 10L): Board =
        Board(
            id = id,
            name = "Board $id",
            color = "#fff",
            description = "Desc",
            owner = userEntity,
            collaborators = mutableSetOf(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun newBoardResponse(id: Long = 10L) = BoardResponseDTO(
        id = id,
        name = "Board $id",
        color = "#fff",
        description = "Desc",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        ownerName = userEntity.name
    )

    @Test
    fun `should create board`() {
        val request = BoardCreateDTO("My Board", "#000", "Desc")
        val response = BoardResponseDTO(
            id = 10L,
            name = request.name,
            color = request.color,
            description = request.description,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            ownerName = userEntity.name
        )


        every { boardService.createBoard(request, userEntity) } returns response

        val result = controller.createBoard(request, userDetails)

        assertEquals(response.id, result.body!!.id)
        assertEquals("My Board", result.body!!.name)

        verify(exactly = 1) {
            boardService.createBoard(request, userEntity)
        }
    }

    @Test
    fun `should return boards for user`() {
        val boards = listOf(newBoardResponse(1), newBoardResponse(2))

        every { boardService.getBoardsForUser(userEntity) } returns boards

        val result = controller.getBoards(userDetails)

        assertEquals(2, result.body!!.size)
        verify { boardService.getBoardsForUser(userEntity) }
    }

    @Test
    fun `should return board by id`() {
        val board = newBoardResponse()

        every {
            boardService.getBoardAccessibleByUser(10L, userEntity)
        } returns board

        val result = controller.getBoard(10L, userDetails)

        assertEquals(10L, result.body!!.id)
        verify { boardService.getBoardAccessibleByUser(10L, userEntity) }
    }

    @Test
    fun `should throw when user has no access to board`() {
        every {
            boardService.getBoardAccessibleByUser(10L, userEntity)
        } throws UnauthorizedAccessException("Forbidden")

        assertThrows<UnauthorizedAccessException> {
            controller.getBoard(10L, userDetails)
        }
    }

    @Test
    fun `should update board`() {
        val request = BoardUpdateDTO("Updated", "#111", "Updated desc")
        val updated = newBoardResponse()

        every {
            boardService.updateBoard(10L, request, userEntity)
        } returns updated

        val result = controller.updateBoard(10L, request, userDetails)

        assertEquals(updated.id, result.body!!.id)
        verify { boardService.updateBoard(10L, request, userEntity) }
    }

    @Test
    fun `should delete board`() {
        every { boardService.deleteBoard(10L, userEntity) } just Runs

        val result = controller.deleteBoard(10L, userDetails)

        assertEquals("Board deleted successfully.", result.body)
        verify { boardService.deleteBoard(10L, userEntity) }
    }

    @Test
    fun `should return shared boards`() {
        val boards = listOf(newBoardResponse(20))

        every {
            boardService.getBoardsSharedWithUser(userEntity)
        } returns boards

        val result = controller.getSharedBoards(userDetails)

        assertEquals(1, result.size)
        verify { boardService.getBoardsSharedWithUser(userEntity) }
    }

    @Test
    fun `should update collaborators`() {
        val dto = BoardCollaboratorUpdateDTO(listOf(2L, 3L))
        val updated = newBoardResponse()

        every {
            boardService.updateCollaborators(10L, dto, userEntity)
        } returns updated

        val result = controller.updateBoardCollaborators(10L, dto, userDetails)

        assertEquals(updated.id, result.body!!.id)
        verify { boardService.updateCollaborators(10L, dto, userEntity) }
    }

    @Test
    fun `should return simple boards`() {
        val boards = listOf(newBoard(1), newBoard(2))

        every { boardRepository.findAll() } returns boards

        val result = controller.getSimpleBoards()

        assertEquals(2, result.size)
        assertEquals("Board 1", result.first().name)
        verify { boardRepository.findAll() }
    }
}
