package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.friend.application.port.FriendshipChecker
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskMember
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.websocket.application.service.TaskWebSocketService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import java.util.Optional
import kotlin.test.assertEquals

class TaskServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMemberRepository: TaskMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationService: NotificationService
    private lateinit var taskWebSocketService: TaskWebSocketService
    private lateinit var boardRepository: BoardRepository
    private lateinit var boardMemberRepository: BoardMemberRepository
    private lateinit var taskSpecificationService: TaskSpecificationService
    private lateinit var friendshipChecker: FriendshipChecker
    private lateinit var activityService: ActivityService
    private lateinit var service: TaskService

    private val owner = User(
        id = 1L,
        name = "Owner",
        email = "owner@test.com",
        passwordHash = "hash"
    )
    private val collaborator = User(
        id = 2L,
        name = "Collab",
        email = "collab@test.com",
        passwordHash = "hash"
    )
    private val board = Board(
        id = 10L,
        name = "Board",
        owner = owner
    )

    @BeforeEach
    fun setup() {
        taskRepository = mockk(relaxed = true)
        taskMemberRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        taskWebSocketService = mockk(relaxed = true)
        boardRepository = mockk(relaxed = true)
        boardMemberRepository = mockk(relaxed = true)
        taskSpecificationService = mockk(relaxed = true)
        friendshipChecker = mockk(relaxed = true)
        activityService = mockk(relaxed = true)

        every { boardMemberRepository.existsByBoardIdAndUserId(any(), any()) } returns true
        every { taskMemberRepository.findAllByTaskId(any()) } returns emptyList()
        every { taskMemberRepository.findByTaskIdAndUserId(any(), any()) } returns null // importante
        every { taskMemberRepository.deleteAll(any<Iterable<TaskMember>>()) } just runs
        every { taskMemberRepository.save(any<TaskMember>()) } answers { firstArg() }
        every { taskMemberRepository.saveAll(any<Iterable<TaskMember>>()) } answers {
            firstArg<Iterable<TaskMember>>().toList()
        }

        service = TaskService(
            taskRepository,
            taskMemberRepository,
            userRepository,
            notificationService,
            taskWebSocketService,
            boardRepository,
            boardMemberRepository,
            taskSpecificationService,
            friendshipChecker,
            activityService
        )
    }

    private fun task(id: Long = 99L): Task = Task(
        id = id,
        title = "Task$id",
        description = "Desc",
        owner = owner,
        board = board,
        status = TaskStatus.TODO,
        priority = TaskPriority.MID
    )

    @Test
    fun `createTask writes owner membership via TaskMemberRepository`() {
        val dto = TaskCreateDTO(title = "New", description = "Desc", boardId = board.id!!)
        val savedTask = task(100L)

        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { taskRepository.save(any()) } returns savedTask

        service.createTask(owner, dto)

        verify(exactly = 1) {
            taskMemberRepository.save(
                match<TaskMember> {
                    it.task.id == savedTask.id &&
                        it.user.id == owner.id &&
                        it.role == MembershipRole.OWNER
                }
            )
        }
    }

    @Test
    fun `updateTaskAssignees syncs assignees only through membership repository`() {
        val existingTask = task(101L)
        every { taskRepository.findById(existingTask.id!!) } returns Optional.of(existingTask)
        every { taskMemberRepository.existsByTaskIdAndUserId(existingTask.id!!, owner.id!!) } returns true
        every { userRepository.findAllById(listOf(collaborator.id!!)) } returns listOf(collaborator)
        every { taskRepository.save(existingTask) } returns existingTask

        service.updateTaskAssignees(existingTask.id!!, listOf(collaborator.id!!), owner)

        verify(exactly = 1) { taskMemberRepository.findAllByTaskId(existingTask.id!!) }
        verify(exactly = 1) { taskMemberRepository.deleteAll(any<Iterable<TaskMember>>()) }
        verify(exactly = 1) { taskMemberRepository.saveAll(any<Iterable<TaskMember>>()) }
        verify(exactly = 1) {
            taskMemberRepository.save(
                match<TaskMember> {
                    it.task.id == existingTask.id &&
                        it.user.id == owner.id &&
                        it.role == MembershipRole.OWNER
                }
            )
        }
    }

    @Test
    fun `updateTask with assignees updates membership not legacy model`() {
        val existingTask = task(103L)
        val request = TaskUpdateDTO(assignees = listOf(collaborator.id!!))

        every { taskRepository.findById(existingTask.id!!) } returns Optional.of(existingTask)
        every { taskMemberRepository.existsByTaskIdAndUserId(existingTask.id!!, owner.id!!) } returns true
        every { userRepository.findAllById(listOf(collaborator.id!!)) } returns listOf(collaborator)
        every { taskRepository.save(existingTask) } returns existingTask
        every { taskWebSocketService.sendTaskUpdate(any()) } just runs

        val updated = service.updateTask(existingTask.id!!, request, owner)

        assertEquals(existingTask.id, updated.id)
        verify(exactly = 1) { taskMemberRepository.saveAll(any<Iterable<TaskMember>>()) }
        verify(exactly = 1) {
            taskMemberRepository.save(
                match<TaskMember> {
                    it.task.id == existingTask.id &&
                        it.user.id == owner.id &&
                        it.role == MembershipRole.OWNER
                }
            )
        }
    }

    @Test
    fun `assignCollaborator saves membership when collaborator is friend`() {
        val existingTask = task(104L)

        every { taskRepository.findById(existingTask.id!!) } returns Optional.of(existingTask)
        every { taskMemberRepository.existsByTaskIdAndUserId(existingTask.id!!, owner.id!!) } returns true
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)
        every { friendshipChecker.areFriends(owner.id!!, collaborator.id!!) } returns true
        every { taskMemberRepository.findByTaskIdAndUserId(existingTask.id!!, collaborator.id!!) } returns null

        service.assignCollaborator(existingTask.id!!, collaborator.email, owner)

        verify(exactly = 1) {
            taskMemberRepository.save(
                match<TaskMember> {
                    it.task.id == existingTask.id &&
                        it.user.id == collaborator.id &&
                        it.role == MembershipRole.COLLABORATOR
                }
            )
        }
        verify(exactly = 1) { notificationService.sendNotification(collaborator.email, any(), any(), existingTask.id) }
    }

    @Test
    fun `assignCollaborator rejects non friend collaborator`() {
        val existingTask = task(105L)

        every { taskRepository.findById(existingTask.id!!) } returns Optional.of(existingTask)
        every { taskMemberRepository.existsByTaskIdAndUserId(existingTask.id!!, owner.id!!) } returns true
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)
        every { friendshipChecker.areFriends(owner.id!!, collaborator.id!!) } returns false

        assertFailsWith<com.synchtask.shared.exception.UnauthorizedAccessException> {
            service.assignCollaborator(existingTask.id!!, collaborator.email, owner)
        }
    }

    @Test
    fun `sync assignees does not add owner membership when owner already exists`() {
        val existingTask = task(106L)
        val ownerMembership = TaskMember(
            task = existingTask,
            user = owner,
            role = MembershipRole.OWNER
        )

        every { taskRepository.findById(existingTask.id!!) } returns Optional.of(existingTask)
        every { taskMemberRepository.existsByTaskIdAndUserId(existingTask.id!!, owner.id!!) } returns true
        every { userRepository.findAllById(listOf(collaborator.id!!)) } returns listOf(collaborator)
        every { taskMemberRepository.findAllByTaskId(existingTask.id!!) } returns listOf(ownerMembership)
        every { taskRepository.save(existingTask) } returns existingTask

        service.updateTaskAssignees(existingTask.id!!, listOf(collaborator.id!!), owner)

        verify(exactly = 0) {
            taskMemberRepository.save(
                match<TaskMember> { it.role == MembershipRole.OWNER && it.task.id == existingTask.id }
            )
        }
    }
}
