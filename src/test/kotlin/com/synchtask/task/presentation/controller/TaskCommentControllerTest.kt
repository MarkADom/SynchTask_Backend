package com.synchtask.task.presentation.controller

import com.synchtask.task.application.dto.TaskCommentCreateDTO
import com.synchtask.task.application.dto.TaskCommentResponseDTO
import com.synchtask.task.application.service.TaskCommentService
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TaskCommentControllerTest {
    private lateinit var taskCommentService: TaskCommentService
    private lateinit var userService: UserService
    private lateinit var controller: TaskCommentController
    private lateinit var userDetails: UserDetails

    @BeforeEach
    fun setup() {
        taskCommentService = mockk()
        userService = mockk()
        controller = TaskCommentController(taskCommentService, userService)
        userDetails = mockk()
    }

    @Test
    fun `should add comment to task`() {
        val taskId = 42L
        val request = TaskCommentCreateDTO("Looks good!")
        val expectedResponse =
            TaskCommentResponseDTO(
                id = 1L,
                taskId = taskId,
                userId = 101L,
                content = "Looks good!",
                createdAt = LocalDateTime.now()
            )
        val actor = User(id = 101L, name = "User", email = "user@email.com", passwordHash = "hash")

        every { userDetails.username } returns "user@email.com"
        every { userService.getUserByEmail("user@email.com") } returns actor
        every { taskCommentService.addComment(taskId, actor, request) } returns expectedResponse

        val response = controller.addComment(taskId, request, userDetails)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expectedResponse, response.body)
        verify(exactly = 1) {
            taskCommentService.addComment(taskId, actor, request)
        }
    }

    @Test
    fun `should return all comments for a task`() {
        val taskId = 99L
        val comment1 = TaskCommentResponseDTO(1, taskId, 201, "First comment", LocalDateTime.now())
        val comment2 = TaskCommentResponseDTO(2, taskId, 202, "Second comment", LocalDateTime.now())
        val comments = listOf(comment1, comment2)

        every { taskCommentService.getCommentsForTask(taskId) } returns comments

        val response = controller.getComments(taskId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body?.size)
        assertEquals(comment1, response.body?.get(0))
        assertEquals(comment2, response.body?.get(1))
        verify(exactly = 1) {
            taskCommentService.getCommentsForTask(taskId)
        }
    }
}
