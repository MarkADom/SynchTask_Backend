package com.synchtask.task.application.service

import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.friend.domain.entity.FriendshipStatus
import com.synchtask.friend.domain.repository.FriendRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.websocket.application.service.TaskWebSocketService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        email = "owner@test.com",
        passwordHash = "hash",
        role = UserRole.USER
    )

    private val otherUser = owner.copy(
        id = 99L,
        email = "other@test.com"
    )

    private val admin = owner.copy(
        id = 500L,
        email = "admin@test.com",
        role = UserRole.ADMIN
    )

    private val board = Board(
        id = 10L,
        name = "Board",
        owner = owner
    )

    private fun newTask(): Task {
        return Task(
            id = 100L,
            title = "Task",
            description = "Desc",
            owner = owner,
            board = board,
            status = TaskStatus.TODO,
            priority = TaskPriority.MID
        )
    }

    @BeforeEach
    fun setup() {
        clearAllMocks()

        taskRepository = mockk()
        userRepository = mockk()
        boardRepository = mockk()
        friendRepository = mockk()
        taskSpecificationService = mockk()
        notificationService = mockk(relaxed = true)
        taskWebSocketService = mockk(relaxed = true)

        taskService = TaskService(
            taskRepository = taskRepository,
            userRepository = userRepository,
            notificationService = notificationService,
            taskWebSocketService = taskWebSocketService,
            boardRepository = boardRepository,
            taskSpecificationService = taskSpecificationService,
            friendRepository = friendRepository
        )
    }

    @Test
    fun `should create task successfully`() {
        val dto = TaskCreateDTO(
            title = "New Task",
            description = "Desc",
            boardId = board.id!!,
            labels = listOf("work"),
            status = TaskStatus.TODO,
            priority = TaskPriority.MID
        )

        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { taskRepository.save(any()) } answers { firstArg() }

        val result = taskService.createTask(owner, dto)

        assertEquals("New Task", result.title)
        assertEquals(owner, result.owner)
        assertEquals(board, result.board)

        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when board does not exist`() {
        val dto = TaskCreateDTO(
            title = "Task",
            description = "Desc",
            boardId = 999L,
            labels = emptyList()
        )

        every { boardRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            taskService.createTask(owner, dto)
        }
    }

    @Test
    fun `should throw when user has no access to board`() {
        val dto = TaskCreateDTO(
            title = "Task",
            description = "Desc",
            boardId = board.id!!,
            labels = emptyList()
        )

        val restrictedBoard = spyk(board)
        every { restrictedBoard.hasAccess(owner) } returns false
        every { boardRepository.findById(board.id!!) } returns Optional.of(restrictedBoard)

        assertFailsWith<UnauthorizedAccessException> {
            taskService.createTask(owner, dto)
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when task not found`() {
        every { taskRepository.findById(404L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            taskService.findTaskById(404L)
        }
    }

    @Test
    fun `should delegate getTaskDetail to findTaskById`() {
        val task = newTask()
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        val result = taskService.getTaskDetail(task.id!!)

        assertEquals(task, result)
    }

    @Test
    fun `should get tasks for user using specification service`() {
        val pageable = mockk<Pageable>()
        val page = mockk<Page<Task>>()

        every {
            taskSpecificationService.findTasksByFilters(owner, null, null, null, null, pageable)
        } returns page

        val result = taskService.getTasksForUser(owner, pageable)

        assertEquals(page, result)
    }

    @Test
    fun `should get tasks with filters using specification service`() {
        val pageable = mockk<Pageable>()
        val page = mockk<Page<Task>>()

        every {
            taskSpecificationService.findTasksByFilters(
                user = owner,
                status = TaskStatus.TODO,
                label = "work",
                assigneeId = 2L,
                boardId = 10L,
                pageable = pageable
            )
        } returns page

        val result = taskService.getTasksWithFilters(
            user = owner,
            status = TaskStatus.TODO,
            label = "work",
            assigneeId = 2L,
            boardId = 10L,
            pageable = pageable
        )

        assertEquals(page, result)
    }

    @Test
    fun `should update task fields when authorized`() {
        val task = newTask()

        val update = TaskUpdateDTO(
            title = "Updated",
            description = "Updated Desc",
            labels = listOf("a", "b"),
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH
        )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskRepository.save(any()) } answers { firstArg() }

        val result = taskService.updateTask(task.id!!, update, owner)

        assertEquals("Updated", result.title)
        assertEquals("Updated Desc", result.description)
        assertEquals(setOf("a", "b"), result.labels)
        assertEquals(TaskStatus.IN_PROGRESS, result.status)
        assertEquals(TaskPriority.HIGH, result.priority)

        verify { taskWebSocketService.sendTaskUpdate(any()) }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when updating task without permission`() {
        val task = newTask()
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            taskService.updateTask(task.id!!, TaskUpdateDTO(title = "Hack"), otherUser)
        }
    }

    @Test
    fun `should throw when updating task with missing assignees`() {
        val task = newTask()
        val update = TaskUpdateDTO(assignees = listOf(1L, 2L, 3L))

        val foundUsers = listOf(
            owner.copy(id = 1L),
            owner.copy(id = 2L)
        )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findAllById(update.assignees!!) } returns foundUsers

        assertFailsWith<ResourceNotFoundException> {
            taskService.updateTask(task.id!!, update, owner)
        }
    }

    @Test
    fun `should update task assignees when all users exist`() {
        val task = newTask()
        val update = TaskUpdateDTO(assignees = listOf(2L, 3L))

        val u2 = owner.copy(id = 2L, email = "u2@test.com")
        val u3 = owner.copy(id = 3L, email = "u3@test.com")

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findAllById(update.assignees!!) } returns listOf(u2, u3)
        every { taskRepository.save(any()) } answers { firstArg() }

        val result = taskService.updateTask(task.id!!, update, owner)

        assertEquals(setOf(u2, u3), result.collaborators.toSet())
        verify { taskWebSocketService.sendTaskUpdate(any()) }
    }

    @Test
    fun `should update task labels when authorized`() {
        val task = newTask()

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskRepository.save(any()) } returns task

        taskService.updateTaskLabels(task.id!!, listOf("x", "y"), owner)

        assertEquals(setOf("x", "y"), task.labels)
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when updating labels without permission`() {
        val task = newTask()

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            taskService.updateTaskLabels(task.id!!, listOf("x"), otherUser)
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `should update task assignees when authorized`() {
        val task = newTask()
        val u2 = owner.copy(id = 2L, email = "u2@test.com")
        val u3 = owner.copy(id = 3L, email = "u3@test.com")

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findAllById(listOf(2L, 3L)) } returns listOf(u2, u3)
        every { taskRepository.save(any()) } returns task

        taskService.updateTaskAssignees(task.id!!, listOf(2L, 3L), owner)

        assertEquals(setOf(u2, u3), task.collaborators.toSet())
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when updating assignees without permission`() {
        val task = newTask()

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            taskService.updateTaskAssignees(task.id!!, listOf(2L), otherUser)
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when updating assignees if some users are missing`() {
        val task = newTask()

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findAllById(listOf(2L, 3L)) } returns listOf(owner.copy(id = 2L))

        assertFailsWith<ResourceNotFoundException> {
            taskService.updateTaskAssignees(task.id!!, listOf(2L, 3L), owner)
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `should return early when updating to same status`() {
        val task = newTask()
        val sameStatus = task.status!!

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        taskService.updateTaskStatus(task.id!!, sameStatus)

        verify(exactly = 0) { notificationService.sendNotification(any(), any(), any(), any()) }
        verify(exactly = 0) { taskWebSocketService.sendTaskUpdate(any()) }
    }

    @Test
    fun `should update status and notify collaborators`() {
        val task = newTask()
        val collaborator = User(
            id = 2L,
            name = "Friend",
            email = "friend@test.com",
            passwordHash = "pw",
            role = UserRole.USER
        )

        task.collaborators.add(collaborator)

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskRepository.save(any()) } returns task

        taskService.updateTaskStatus(task.id!!, TaskStatus.COMPLETED)

        verify {
            notificationService.sendNotification(
                userEmail = collaborator.email,
                message = any(),
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }

        verify { taskWebSocketService.sendTaskUpdate(any()) }
    }

    @Test
    fun `should throw when collaborator user does not exist`() {
        val task = newTask()

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail("missing@test.com") } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            taskService.assignCollaborator(task.id!!, "missing@test.com")
        }
    }

    @Test
    fun `should throw when assigning non friend`() {
        val task = newTask()
        val collaborator = owner.copy(id = 3L, email = "stranger@test.com")

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)

        every {
            friendRepository.findFriendsByRequesterEmailOrFriendEmailAndStatus(
                requesterEmail = any(),
                friendEmail = any(),
                status = FriendshipStatus.ACCEPTED
            )
        } returns emptyList()

        assertFailsWith<UnauthorizedAccessException> {
            taskService.assignCollaborator(task.id!!, collaborator.email)
        }
    }

    @Test
    fun `should return early when collaborator already assigned`() {
        val task = newTask()
        val collaborator = owner.copy(id = 3L, email = "friend@test.com")

        task.collaborators.add(collaborator)

        val friendship = com.synchtask.friend.domain.entity.Friend(
            id = 1L,
            requester = owner,
            friend = collaborator,
            status = FriendshipStatus.ACCEPTED
        )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)

        every {
            friendRepository.findFriendsByRequesterEmailOrFriendEmailAndStatus(
                requesterEmail = task.owner.email,
                friendEmail = task.owner.email, // matches current TaskService implementation
                status = FriendshipStatus.ACCEPTED
            )
        } returns listOf(friendship)

        taskService.assignCollaborator(task.id!!, collaborator.email)

        verify(exactly = 0) { taskRepository.save(any()) }
        verify(exactly = 0) {
            notificationService.sendNotification(
                userEmail = any(),
                message = any(),
                type = any(),
                groupId = any()
            )
        }
    }

    @Test
    fun `should assign collaborator and send notification when friend`() {
        val task = newTask()
        val collaborator = owner.copy(id = 3L, email = "friend@test.com")

        val friendship = com.synchtask.friend.domain.entity.Friend(
            id = 1L,
            requester = owner,
            friend = collaborator,
            status = FriendshipStatus.ACCEPTED
        )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)

        every {
            friendRepository.findFriendsByRequesterEmailOrFriendEmailAndStatus(
                requesterEmail = task.owner.email,
                friendEmail = task.owner.email, // matches current TaskService implementation
                status = FriendshipStatus.ACCEPTED
            )
        } returns listOf(friendship)

        every { taskRepository.save(any()) } returns task

        taskService.assignCollaborator(task.id!!, collaborator.email)

        kotlin.test.assertTrue(task.collaborators.contains(collaborator))

        verify(exactly = 1) {
            notificationService.sendNotification(
                userEmail = collaborator.email,
                message = any(),
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }
        verify(exactly = 1) { taskRepository.save(any()) }
    }


    @Test
    fun `should delete task when authorized`() {
        val task = newTask()

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskRepository.delete(task) } just Runs

        taskService.deleteTask(task.id!!, owner)

        verify(exactly = 1) { taskRepository.delete(task) }
    }

    @Test
    fun `should throw when deleting task without permission`() {
        val task = newTask()
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            taskService.deleteTask(task.id!!, otherUser)
        }

        verify(exactly = 0) { taskRepository.delete(any()) }
    }

    @Test
    fun `canAccessTask should allow admin`() {
        val task = newTask()
        assertTrue(taskService.canAccessTask(task, admin))
    }

    @Test
    fun `canAccessTask should allow task owner`() {
        val task = newTask()
        assertTrue(taskService.canAccessTask(task, owner))
    }

    @Test
    fun `canAccessTask should allow collaborator`() {
        val task = newTask()
        val collaborator = owner.copy(id = 2L, email = "collab@test.com")
        task.collaborators.add(collaborator)

        assertTrue(taskService.canAccessTask(task, collaborator))
    }

    @Test
    fun `canAccessTask should allow board owner`() {
        val task = newTask()
        assertTrue(taskService.canAccessTask(task, owner))
    }

    @Test
    fun `canAccessTask should deny unrelated user`() {
        val task = newTask()
        val unrelated = owner.copy(id = 777L, email = "unrelated@test.com")

        assertFalse(taskService.canAccessTask(task, unrelated))
    }
}
