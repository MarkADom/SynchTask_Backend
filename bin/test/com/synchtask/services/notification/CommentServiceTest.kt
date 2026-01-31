package com.synchtask.services.notification

import com.synchtask.entities.Task
import com.synchtask.entities.TaskComment
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.TaskCommentRepository
import com.synchtask.repositories.TaskRepository
import com.synchtask.repositories.UserRepository
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

    private val user = User(
        id = 1L,
        name = "Alice",
        email = "alice@example.com",
        passwordHash = "123456",
        role = UserRole.USER
    )

    private val task = Task(
        id = 1L,
        title = "Test Task",
        description = "A task for testing",
        owner = user
    )

    @BeforeEach
    fun setup() {
        taskCommentRepository = mockk()
        taskRepository = mockk()
        userRepository = mockk()
        commentService = CommentService(taskCommentRepository, taskRepository, userRepository)
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
