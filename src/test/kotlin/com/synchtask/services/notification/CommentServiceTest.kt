package com.synchtask.services.notification

import com.synchtask.board.domain.entity.Board
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.notification.application.service.CommentService
import com.synchtask.task.domain.repository.TaskCommentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.repository.UserRepository
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

class CommentServiceTest {

    private lateinit var taskCommentRepository: TaskCommentRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var commentService: CommentService

    private val owner = User(
        id = 1L,
        name = "Owner",
        email = "owner@test.com",
        passwordHash = "hash",
        role = UserRole.USER
    )

    private val board = Board(
        id = 10L,
        name = "Test Board",
        owner = owner
    )

    private val user = User(
        id = 2L,
        name = "Alice",
        email = "alice@example.com",
        passwordHash = "123456",
        role = UserRole.USER
    )

    private val task = Task(
        id = 1L,
        title = "Test Task",
        description = "A task for testing",
        owner = owner,
        board = board
    )

    @BeforeEach
    fun setup() {
        taskCommentRepository = mockk()
        taskRepository = mockk()
        userRepository = mockk()
        commentService = CommentService(
            taskCommentRepository,
            taskRepository,
            userRepository
        )
    }

    @Test
    fun `should add comment to task`() {
        val content = "This is a comment"
        val commentSlot = slot<TaskComment>()

        every { taskRepository.findById(1L) } returns Optional.of(task)
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(user)
        every { taskCommentRepository.save(capture(commentSlot)) } answers { commentSlot.captured }

        val result = commentService.addComment(1L, "alice@example.com", content)

        assertEquals(task, result.task)
        assertEquals(user, result.user)
        assertEquals(content, result.content)

        verify(exactly = 1) { taskCommentRepository.save(any()) }
    }

    @Test
    fun `should throw if task not found`() {
        every { taskRepository.findById(1L) } returns Optional.empty()

        val ex = assertThrows<ResourceNotFoundException> {
            commentService.addComment(1L, "alice@example.com", "Comment")
        }

        assertEquals("Task not found", ex.message)
    }

    @Test
    fun `should throw if user not found`() {
        every { taskRepository.findById(1L) } returns Optional.of(task)
        every { userRepository.findByEmail("alice@example.com") } returns Optional.empty()

        val ex = assertThrows<ResourceNotFoundException> {
            commentService.addComment(1L, "alice@example.com", "Comment")
        }

        assertEquals("User not found: alice@example.com", ex.message)
    }
}
