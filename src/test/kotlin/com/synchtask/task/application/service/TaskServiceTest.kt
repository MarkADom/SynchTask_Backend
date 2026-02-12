package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.friend.application.port.FriendshipChecker
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.websocket.application.service.TaskWebSocketService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import kotlin.test.assertEquals

class TaskServiceTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationService: NotificationService
    private lateinit var taskWebSocketService: TaskWebSocketService
    private lateinit var boardRepository: BoardRepository
    private lateinit var taskSpecificationService: TaskSpecificationService
    private lateinit var friendshipChecker: FriendshipChecker
    private lateinit var activityService: ActivityService
    private lateinit var service: TaskService

    private val owner = User(id = 1L, name = "Owner", email = "owner@test.com", passwordHash = "hash")
    private val collab = User(id = 2L, name = "Collab", email = "collab@test.com", passwordHash = "hash")
    private val board = Board(id = 10L, name = "Board", owner = owner)

    @BeforeEach
    fun setup() {
        taskRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        taskWebSocketService = mockk(relaxed = true)
        boardRepository = mockk(relaxed = true)
        taskSpecificationService = mockk(relaxed = true)
        friendshipChecker = mockk(relaxed = true)
        activityService = mockk(relaxed = true)

        service = TaskService(
            taskRepository,
            userRepository,
            notificationService,
            taskWebSocketService,
            boardRepository,
            taskSpecificationService,
            friendshipChecker,
            activityService
        )
    }

    private fun task(id: Long = 1L, owner: User = this.owner) = Task(
        id = id,
        title = "Task$id",
        description = "Desc",
        owner = owner,
        board = board,
        collaborators = mutableSetOf(),
        status = TaskStatus.TODO,
        priority = TaskPriority.MID
    )

    @Test
    fun `should create task`() {
        val dto = TaskCreateDTO(title = "New", description = "Desc", boardId = board.id!!)
        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { taskRepository.save(any()) } answers {
            val t = firstArg<Task>()
            Task(
                id = 99L,
                title = t.title,
                description = t.description,
                owner = t.owner,
                collaborators = t.collaborators,
                labels = t.labels,
                status = t.status,
                priority = t.priority,
                comments = t.comments,
                createdAt = t.createdAt,
                updatedAt = t.updatedAt,
                board = t.board
            )
        }

        val result = service.createTask(owner, dto)

        assertEquals("New", result.title)
        assertEquals(99L, result.id)
    }

    @Test
    fun `should throw when board not found on create`() {
        val dto = TaskCreateDTO(title = "New", description = "Desc", boardId = 999L)
        every { boardRepository.findById(999L) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.createTask(owner, dto)
        }
    }

    @Test
    fun `should update task`() {
        val existing = task()
        val req = TaskUpdateDTO(title = "Updated", description = "D2", status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH)

        every { taskRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { taskRepository.save(any()) } answers { firstArg() }

        val result = service.updateTask(existing.id!!, req, owner)

        assertEquals("Updated", result.title)
        assertEquals(TaskPriority.HIGH, result.priority)
    }

    @Test
    fun `should throw unauthorized when updating task without access`() {
        val existing = task(owner = collab)
        every { taskRepository.findById(existing.id!!) } returns Optional.of(existing)

        assertThrows<UnauthorizedAccessException> {
            service.updateTask(existing.id!!, TaskUpdateDTO(title = "X"), owner)
        }
    }

    @Test
    fun `should update task labels`() {
        val existing = task()
        every { taskRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { taskRepository.save(any()) } answers { firstArg() }

        service.updateTaskLabels(existing.id!!, listOf("backend", "urgent"), owner)

        assertEquals(setOf("backend", "urgent"), existing.labels)
    }

    @Test
    fun `should update task assignees`() {
        val existing = task()
        every { taskRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { userRepository.findAllById(listOf(2L)) } returns listOf(collab)
        every { taskRepository.save(any()) } answers { firstArg() }

        service.updateTaskAssignees(existing.id!!, listOf(2L), owner)

        assertEquals(1, existing.collaborators.size)
    }

    @Test
    fun `should update status`() {
        val existing = task()
        every { taskRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { taskRepository.save(any()) } answers { firstArg() }

        service.updateTaskStatus(existing.id!!, TaskStatus.IN_PROGRESS, owner)

        assertEquals(TaskStatus.IN_PROGRESS, existing.status)
    }

    @Test
    fun `should assign collaborator`() {
        val existing = task()
        every { taskRepository.findById(existing.id!!) } returns Optional.of(existing)
        every { userRepository.findByEmail(collab.email) } returns Optional.of(collab)
        every { friendshipChecker.areFriends(owner.id!!, collab.id!!) } returns true
        every { taskRepository.save(any()) } answers { firstArg() }

        service.assignCollaborator(existing.id!!, collab.email, owner)

        assertEquals(1, existing.collaborators.size)
    }

    @Test
    fun `should return tasks with filters`() {
        val page = PageImpl(listOf(task(1L), task(2L)))
        every { taskSpecificationService.findTasksByFilters(owner, null, null, null, null, any()) } returns page

        val result = service.getTasksWithFilters(owner, null, null, null, null, PageRequest.of(0, 20))

        assertEquals(2, result.totalElements)
    }
}
