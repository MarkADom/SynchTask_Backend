package com.synchtask.board.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.application.dto.BoardCollaboratorUpdateDTO
import com.synchtask.board.application.dto.BoardCreateDTO
import com.synchtask.board.application.dto.BoardUpdateDTO
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.*

class BoardServiceTest {
    private lateinit var boardRepository: BoardRepository
    private lateinit var userRepository: UserRepository
    private lateinit var activityService: ActivityService
    private lateinit var service: BoardService

    private val owner = newUser(1L, "owner@test.com")
    private val other = newUser(2L, "other@test.com")

    @BeforeEach
    fun setup() {
        clearAllMocks()

        boardRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        activityService = mockk(relaxed = true)
        service = BoardService(boardRepository, userRepository, activityService)
    }

    private fun newUser(id: Long, email: String) = User(
        id = id,
        name = "User",
        email = email,
        passwordHash = "hash"
    )

    private fun newBoard(id: Long = 10L, owner: User = this.owner,): Board = Board(
        id = id,
        name = "Board",
        color = "#fff",
        description = "Desc",
        owner = owner,
        collaborators = mutableSetOf(),
        createdAt = java.time.LocalDateTime.now(),
        updatedAt = java.time.LocalDateTime.now()
    )

    @Test
    fun `should create board`() {
        val dto = BoardCreateDTO("My Board", "#000", "Desc")

        every { boardRepository.save(any()) } answers {
            val b = firstArg<Board>()
            Board(
                id = 10L,
                name = b.name,
                color = b.color,
                description = b.description,
                owner = b.owner,
                collaborators = b.collaborators,
                createdAt = b.createdAt,
                updatedAt = b.updatedAt
            )
        }

        val result = service.createBoard(dto, owner)

        assertEquals("My Board", result.name)
        verify(exactly = 1) { boardRepository.save(any()) }
    }

    @Test
    fun `should return boards for user`() {
        every { boardRepository.findByOwner(owner) } returns listOf(newBoard())

        val result = service.getBoardsForUser(owner)

        assertEquals(1, result.size)
    }

    @Test
    fun `should return board when user has access`() {
        val board = newBoard(owner = owner)

        every { boardRepository.findById(any()) } returns Optional.of(board)

        val result = service.getBoardAccessibleByUser(board.id!!, owner)

        assertEquals(board.id, result.id)
    }

    @Test
    fun `should throw when user has no access to board`() {
        val board = newBoard(owner = owner)

        every { boardRepository.findById(any()) } returns Optional.of(board)

        assertFailsWith<UnauthorizedAccessException> {
            service.getBoardAccessibleByUser(board.id!!, other)
        }
    }

    @Test
    fun `should throw when board does not exist`() {
        every { boardRepository.findById(any()) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.getBoardAccessibleByUser(99L, owner)
        }
    }

    @Test
    fun `should update board when owner`() {
        val board = newBoard()

        every { boardRepository.findById(any()) } returns Optional.of(board)
        every { boardRepository.save(any()) } answers { firstArg() }

        val dto =
            BoardUpdateDTO(
                name = "New",
                color = "#111",
                description = "Updated"
            )

        val result = service.updateBoard(board.id!!, dto, owner)

        assertEquals("New", result.name)
        assertEquals("#111", result.color)
        assertEquals("Updated", result.description)

        verify(exactly = 1) { boardRepository.save(board) }
    }

    @Test
    fun `should throw when updating board by non owner`() {
        val board = newBoard()

        every { boardRepository.findById(any()) } returns Optional.of(board)

        assertFailsWith<UnauthorizedAccessException> {
            service.updateBoard(board.id!!, BoardUpdateDTO("X", null, null), other)
        }
    }

    @Test
    fun `should delete board when owner`() {
        val board = newBoard()

        every { boardRepository.findById(any()) } returns Optional.of(board)
        every { boardRepository.delete(board) } just Runs

        service.deleteBoard(board.id!!, owner)

        verify(exactly = 1) { boardRepository.delete(board) }
    }

    @Test
    fun `should throw when deleting board by non owner`() {
        val board = newBoard()

        every { boardRepository.findById(any()) } returns Optional.of(board)

        assertFailsWith<UnauthorizedAccessException> {
            service.deleteBoard(board.id!!, other)
        }

        verify(exactly = 0) { boardRepository.delete(any()) }
    }

    @Test
    fun `should get boards shared with user`() {
        every { boardRepository.findByCollaboratorsContaining(other) } returns listOf(newBoard())

        val result = service.getBoardsSharedWithUser(other)

        assertEquals(1, result.size)
    }

    @Test
    fun `should update collaborators when owner`() {
        val board = newBoard()
        val collaborator = newUser(3L, "c@test.com")

        every { boardRepository.findById(any()) } returns Optional.of(board)
        every { userRepository.findAllById(listOf(3L)) } returns listOf(collaborator)
        every { boardRepository.save(any()) } answers { firstArg() }

        val dto = BoardCollaboratorUpdateDTO(listOf(3L))

        service.updateCollaborators(board.id!!, dto, owner)

        assertEquals(1, board.collaborators.size)
        assertTrue(board.collaborators.contains(collaborator))
    }

    @Test
    fun `should throw when updating collaborators by non owner`() {
        val board = newBoard()

        every { boardRepository.findById(any()) } returns Optional.of(board)

        assertFailsWith<UnauthorizedAccessException> {
            service.updateCollaborators(board.id!!, BoardCollaboratorUpdateDTO(emptyList()), other)
        }
    }
}
