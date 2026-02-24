package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskLink
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskLinkRepository
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.*

class TaskLinkServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskLinkRepository: TaskLinkRepository
    private lateinit var taskMemberRepository: TaskMemberRepository
    private lateinit var boardMemberRepository: BoardMemberRepository
    private lateinit var activityService: ActivityService
    private lateinit var service: TaskLinkService
    private lateinit var owner: User
    private lateinit var other: User
    private lateinit var board: Board
    private lateinit var task: Task

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskLinkRepository = mockk()
        taskMemberRepository = mockk(relaxed = true)
        boardMemberRepository = mockk(relaxed = true)
        activityService = mockk(relaxed = true)


        every { taskMemberRepository.existsByTaskIdAndUserId(any(), any()) } returns false
        every { boardMemberRepository.existsByBoardIdAndUserId(any(), any()) } returns false

        service = TaskLinkService(
            taskRepository,
            taskMemberRepository,
            boardMemberRepository,
            taskLinkRepository,
            activityService
        )

        owner = newUser(id = 1L, email = "owner@test.com")
        other = newUser(id = 2L, email = "other@test.com")
        board = newBoard(id = 50L, owner = owner)
        task = newTask(id = 10L, owner = owner, board = board)
    }

    private fun newUser(
        id: Long,
        email: String,
    ): User = User(
        id = id,
        name = "User",
        email = email,
        passwordHash = "hash",
        role = UserRole.USER
    )

    private fun newBoard(
        id: Long,
        owner: User,
    ): Board = Board(
        id = id,
        name = "Board",
        owner = owner
    )

    private fun newTask(
        id: Long,
        owner: User,
        board: Board,
    ): Task = Task(
        id = id,
        title = "Task",
        description = "Desc",
        owner = owner,
        collaborators = mutableSetOf(),
        labels = mutableSetOf(),
        status = TaskStatus.TODO,
        priority = TaskPriority.MID,
        board = board
    )

    @Test
    fun `should add link when user is owner`() {
        val title = "Docs"
        val url = "https://example.com"
        val now = LocalDateTime.of(2026, 2, 4, 0, 0, 0)

        val savedLink =
            TaskLink(
                id = 100L,
                task = task,
                title = title,
                url = url,
                createdAt = now
            )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskLinkRepository.save(any()) } returns savedLink

        val result =
            service.addLink(
                taskId = task.id!!,
                title = title,
                url = url,
                user = owner
            )

        assertEquals(100L, result.id)
        assertEquals(title, result.title)
        assertEquals(url, result.url)
        assertEquals(now, result.createdAt)

        verify(exactly = 1) { taskLinkRepository.save(any()) }
    }

    @Test
    fun `should throw when adding link and task does not exist`() {
        every { taskRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.addLink(999L, "Docs", "https://example.com", owner)
        }

        verify(exactly = 0) { taskLinkRepository.save(any()) }
    }

    @Test
    fun `should throw when adding link and user is not owner`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            service.addLink(task.id!!, "Docs", "https://example.com", other)
        }

        verify(exactly = 0) { taskLinkRepository.save(any()) }
    }

    @Test
    fun `should add link when user has task membership`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskMemberRepository.existsByTaskIdAndUserId(task.id!!, other.id!!) } returns true
        every { taskLinkRepository.save(any()) } answers { firstArg() }

        val result = service.addLink(task.id!!, "Docs", "https://example.com", other)

        assertEquals("Docs", result.title)
    }

    @Test
    fun `should list links for task`() {
        val l1 = TaskLink(id = 1L, task = task, title = "A", url = "https://a.com")
        val l2 = TaskLink(id = 2L, task = task, title = "B", url = "https://b.com")

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskLinkRepository.findAllByTask(task) } returns listOf(l1, l2)

        val result = service.listLinks(task.id!!, owner)

        assertEquals(2, result.size)
        assertEquals("A", result[0].title)
        assertEquals("B", result[1].title)
    }

    @Test
    fun `should throw when listing links and task does not exist`() {
        every { taskRepository.findById(404L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.listLinks(404L, owner)
        }

        verify(exactly = 0) { taskLinkRepository.findAllByTask(any()) }
    }

    @Test
    fun `should delete link when found`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskLinkRepository.deleteByTaskAndId(task, 100L) } returns 1

        service.removeLink(task.id!!, 100L, owner)

        verify(exactly = 1) { taskLinkRepository.deleteByTaskAndId(task, 100L) }
    }

    @Test
    fun `should throw when link does not exist`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskLinkRepository.deleteByTaskAndId(task, 999L) } returns 0

        assertFailsWith<ResourceNotFoundException> {
            service.removeLink(task.id!!, 999L, owner)
        }
    }

    @Test
    fun `should throw when removing link and user is not owner`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            service.removeLink(task.id!!, 100L, other)
        }

        verify(exactly = 0) { taskLinkRepository.deleteByTaskAndId(any(), any()) }
    }

    @Test
    fun `should throw when removing link and task does not exist`() {
        every { taskRepository.findById(404L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.removeLink(404L, 100L, owner)
        }

        verify(exactly = 0) { taskLinkRepository.deleteByTaskAndId(any(), any()) }
    }
}
