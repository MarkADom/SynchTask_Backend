package com.synchtask.task.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.task.domain.entity.TaskMember
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

class TaskCommentServiceTest {
    private lateinit var taskCommentRepository: TaskCommentRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMemberRepository: TaskMemberRepository
    private lateinit var boardMemberRepository: BoardMemberRepository
    private lateinit var notificationService: NotificationService
    private lateinit var activityService: ActivityService
    private lateinit var service: TaskCommentService

    private val user =
        User(
            id = 1L,
            name = "User",
            email = "user@example.com",
            passwordHash = "123"
        )

    private val boardOwner =
        User(
            id = 99L,
            name = "Board Owner",
            email = "owner@board.com",
            passwordHash = "hash"
        )

    private val board =
        Board(
            id = 100L,
            name = "Test Board",
            owner = boardOwner
        )

    private val collaborator =
        User(
            id = 2L,
            name = "Collaborator",
            email = "collab@example.com",
            passwordHash = "456"
        )

    private val task =
        Task(
            id = 10L,
            title = "Title",
            description = "Desc",
            owner = user,
            status = TaskStatus.TODO,
            board = board
        )

    @BeforeEach
    fun setup() {
        taskCommentRepository = mockk()
        taskRepository = mockk()
        taskMemberRepository = mockk(relaxed = true)
        boardMemberRepository = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        activityService = mockk(relaxed = true)

        every { taskMemberRepository.existsByTaskIdAndUserId(any(), any()) } returns false
        every { taskMemberRepository.findAllByTaskId(any()) } returns emptyList()
        every { boardMemberRepository.existsByBoardIdAndUserId(any(), any()) } returns false

        service =
            TaskCommentService(
                taskCommentRepository,
                taskRepository,
                taskMemberRepository,
                boardMemberRepository,
                notificationService,
                activityService
            )
    }

    @Test
    fun `should add comment successfully and send notifications`() {
        val request = TaskCommentCreateDTO(content = "New comment")

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskMemberRepository.existsByTaskIdAndUserId(task.id!!, user.id!!) } returns true
        every { taskMemberRepository.findAllByTaskId(task.id!!) } returns listOf(
            TaskMember(task = task, user = collaborator, role = MembershipRole.COLLABORATOR)
        )

        val slot = slot<TaskComment>()
        every { taskCommentRepository.save(capture(slot)) } answers {
            slot.captured.apply {
                val field = TaskComment::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 1L)
            }
        }

        val result = service.addComment(task.id!!, user, request)

        assertEquals("New comment", result.content)
        assertEquals(task.id, result.taskId)
        assertEquals(user.id, result.userId)

        verify(exactly = 1) {
            notificationService.sendNotification(
                userEmail = collaborator.email,
                message = any(),
                type = any(),
                groupId = task.id
            )
        }
    }

    @Test
    fun `should throw if task not found`() {
        every { taskRepository.findById(any()) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.addComment(99L, user, TaskCommentCreateDTO("Hi"))
        }
    }

    @Test
    fun `should throw if user is not authorized to comment`() {
        val stranger = User(
            id = 10L,
            name = "NoPerm",
            email = "no@access.com",
            passwordHash = "123"
        )
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertThrows<UnauthorizedAccessException> {
            service.addComment(task.id!!, stranger, TaskCommentCreateDTO("Oi"))
        }
    }

    @Test
    fun `should return all comments for a task`() {
        val comment =
            TaskComment(
                id = 1L,
                content = "Test comment",
                user = user,
                task = task
            )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskCommentRepository.findByTaskOrderByCreatedAtAsc(task) } returns listOf(comment)

        val result = service.getCommentsForTask(task.id!!)

        assertEquals(1, result.size)
        assertEquals("Test comment", result.first().content)
    }

    @Test
    fun `should add comment when user has task membership`() {
        val stranger = User(
            id = 10L,
            name = "NoPerm",
            email = "no@access.com",
            passwordHash = "123"
        )
        val membershipTask =
            Task(
                id = task.id,
                title = task.title,
                description = task.description,
                owner = task.owner,
                status = task.status,
                board = task.board
            )
        every { taskRepository.findById(task.id!!) } returns Optional.of(membershipTask)
        every { taskMemberRepository.existsByTaskIdAndUserId(task.id!!, stranger.id!!) } returns true
        every { taskMemberRepository.findAllByTaskId(task.id!!) } returns listOf(
            TaskMember(task = membershipTask, user = collaborator, role = MembershipRole.COLLABORATOR)
        )

        val slot = slot<TaskComment>()
        every { taskCommentRepository.save(capture(slot)) } answers {
            slot.captured.apply {
                val field = TaskComment::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 2L)
            }
        }

        val result = service.addComment(task.id!!, stranger, TaskCommentCreateDTO("membership"))

        assertEquals("membership", result.content)
        verify(exactly = 1) {
            notificationService.sendNotification(
                userEmail = collaborator.email,
                message = any(),
                type = any(),
                groupId = task.id
            )
        }
    }
}
