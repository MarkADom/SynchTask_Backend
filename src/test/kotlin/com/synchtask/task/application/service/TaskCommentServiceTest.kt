package com.synchtask.task.application.service

import com.synchtask.board.domain.entity.Board
import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.user.domain.entity.User
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.*
import kotlin.test.assertEquals

class TaskCommentServiceTest {

    private lateinit var taskCommentRepository: TaskCommentRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationService: NotificationService
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var service: TaskCommentService

    private val userEmail = "user@example.com"

    private val user = User(
        id = 1L,
        name = "User",
        email = userEmail,
        passwordHash = "123"
    )

    private val boardOwner = User(
        id = 99L,
        name = "Board Owner",
        email = "owner@board.com",
        passwordHash = "hash"
    )

    private val board = Board(
        id = 100L,
        name = "Test Board",
        owner = boardOwner
    )

    private val collaborator = User(
        id = 2L,
        name = "Collaborator",
        email = "collab@example.com",
        passwordHash = "456"
    )

    private val task = Task(
        id = 10L,
        title = "Title",
        description = "Desc",
        owner = user,
        collaborators = mutableSetOf(collaborator),
        status = TaskStatus.TODO,
        board = board
    )

    @BeforeEach
    fun setup() {
        taskCommentRepository = mockk()
        taskRepository = mockk()
        userRepository = mockk()
        notificationService = mockk(relaxed = true)
        messagingTemplate = mockk(relaxed = true)

        service = TaskCommentService(
            taskCommentRepository,
            taskRepository,
            userRepository,
            notificationService,
            messagingTemplate
        )
    }

    @Test
    fun `should add comment successfully and send notifications`() {
        val request = TaskCommentCreateDTO(content = "New comment")

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)

        val slot = slot<TaskComment>()

        every { taskCommentRepository.save(capture(slot)) } answers {
            slot.captured.apply {
                val field = TaskComment::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 1L)
            }
        }

        val result = service.addComment(task.id!!, userEmail, request)

        assertEquals("New comment", result.content)
        assertEquals(task.id, result.taskId)
        assertEquals(user.id, result.userId)

        verify {
            messagingTemplate.convertAndSend(
                eq("/topic/tasks/${task.id}/comments"),
                any<TaskCommentResponseDTO>()
            )
        }

        verify(exactly = 1) {
            notificationService.sendNotification(
                userEmail = user.email,
                message = any(),
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }

        verify(exactly = 1) {
            notificationService.sendNotification(
                userEmail = collaborator.email,
                message = any(),
                type = NotificationType.TASK_UPDATE,
                groupId = task.id
            )
        }
    }

    @Test
    fun `should throw if task not found`() {
        every { taskRepository.findById(any()) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.addComment(99L, userEmail, TaskCommentCreateDTO("Hi"))
        }
    }

    @Test
    fun `should throw if user not found`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(userEmail) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.addComment(task.id!!, userEmail, TaskCommentCreateDTO("Oi"))
        }
    }

    @Test
    fun `should throw if user is not authorized to comment`() {
        val stranger = User(id = 10L, name = "NoPerm", email = "no@access.com", passwordHash = "123")
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(stranger.email) } returns Optional.of(stranger)

        assertThrows<IllegalAccessException> {
            service.addComment(task.id!!, stranger.email, TaskCommentCreateDTO("Oi"))
        }
    }

    @Test
    fun `should return all comments for a task`() {
        val comment = TaskComment(
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
}
