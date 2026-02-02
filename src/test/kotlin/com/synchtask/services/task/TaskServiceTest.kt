package com.synchtask.services.task

import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.*
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.task.application.service.TaskService
import com.synchtask.task.application.service.TaskSpecificationService
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.websocket.TaskWebSocketService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskServiceTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var boardRepository: BoardRepository
    private lateinit var friendRepository: FriendRepository
    private lateinit var taskSpecificationService: TaskSpecificationService
    private lateinit var notificationService: NotificationService
    private lateinit var taskWebSocketService: TaskWebSocketService
    private lateinit var taskService: TaskService

    private val owner = User(
        id = 1L,
        name = "Owner",
        email = "owner@example.com",
        passwordHash = "hash",
        role = UserRole.USER
    )

    private val board = Board(
        id = 100L,
        name = "Board",
        owner = owner
    )

    private val task = Task(
        id = 10L,
        title = "Old Title",
        description = "Old Desc",
        owner = owner,
        board = board
    )

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        userRepository = mockk()
        boardRepository = mockk()
        friendRepository = mockk()
        taskSpecificationService = mockk()
        notificationService = mockk(relaxed = true)
        taskWebSocketService = mockk(relaxed = true)

        taskService = TaskService(
            taskRepository,
            userRepository,
            notificationService,
            taskWebSocketService,
            boardRepository,
            taskSpecificationService,
            friendRepository
        )
    }

    @Test
    fun `should create task`() {
        val dto = TaskCreateDTO(
            title = "New",
            description = "Desc",
            boardId = board.id!!,
            labels = emptyList()
        )

        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { taskRepository.save(any()) } answers { firstArg() }

        val result = taskService.createTask(owner, dto)

        assertEquals("New", result.title)
        assertEquals("Desc", result.description)
        assertEquals(board, result.board)
        assertEquals(owner, result.owner)

        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when task not found`() {
        every { taskRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            taskService.findTaskById(99L)
        }
    }
}
