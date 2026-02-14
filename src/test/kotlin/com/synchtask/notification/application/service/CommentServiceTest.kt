package com.synchtask.notification.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.entity.Board
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.service.TaskCommentService
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

class CommentServiceTest {
    private lateinit var taskCommentRepository: TaskCommentRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationService: NotificationService
    private lateinit var activityService: ActivityService
    private lateinit var commentService: TaskCommentService

    private val owner =
        User(
            id = 1L,
            name = "Owner",
            email = "owner@test.com",
            passwordHash = "hash",
            role = UserRole.USER
        )

    private val board =
        Board(
            id = 10L,
            name = "Test Board",
            owner = owner
        )

    private val user =
        User(
            id = 2L,
            name = "Alice",
            email = "alice@example.com",
            passwordHash = "123456",
            role = UserRole.USER
        )

    private val task =
        Task(
            id = 1L,
            title = "Test Task",
            description = "A task for testing",
            owner = owner,
            board = board,
            collaborators = mutableSetOf(user)
        )

    @BeforeEach
    fun setup() {
        taskCommentRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        activityService = mockk(relaxed = true)
        commentService =
            TaskCommentService(
                taskCommentRepository,
                taskRepository,
                userRepository,
                notificationService,
                activityService
            )
    }

    @Test
    fun `should add comment to task`() {
        val content = "This is a comment"

        every { taskRepository.findById(1L) } returns Optional.of(task)
        every { taskCommentRepository.save(any()) } answers {
            val original = firstArg<TaskComment>()
            TaskComment(
                id = 1L,
                task = original.task,
                user = original.user,
                content = original.content,
                createdAt = original.createdAt
            )
        }

        val result = commentService.addComment(1L, user, TaskCommentCreateDTO(content))

        assertEquals(task.id, result.taskId)
        assertEquals(user.id, result.userId)
        assertEquals(content, result.content)
    }

    @Test
    fun `should throw if task not found`() {
        every { taskRepository.findById(1L) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            commentService.addComment(1L, user, TaskCommentCreateDTO("Comment"))
        }
    }
}
