package com.synchtask.controllers

import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.entities.*
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.task.application.service.TaskService
import com.synchtask.services.user.UserService
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.presentation.controller.TaskController
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TaskControllerTest {

    private lateinit var taskService: TaskService
    private lateinit var userService: UserService
    private lateinit var controller: TaskController

    private lateinit var userEntity: com.synchtask.entities.User
    private lateinit var userDetails: UserDetails
    private lateinit var board: Board

    @BeforeEach
    fun setup() {
        taskService = mockk()
        userService = mockk()
        controller = TaskController(taskService, userService)

        userEntity = com.synchtask.entities.User(
            id = 1L,
            email = "user@synchtask.com",
            name = "User",
            passwordHash = "hash",
            role = UserRole.USER
        )

        userDetails = User(
            userEntity.email,
            "hash",
            emptyList()
        )

        board = Board(
            id = 10L,
            name = "Main Board",
            owner = userEntity,
            project = null
        )
    }

    @Test
    fun `should create task successfully`() {
        val request = TaskCreateDTO(
            title = "Test Task",
            description = "Test Description",
            assignees = emptyList(),
            labels = listOf("backend"),
            boardId = board.id!!
        )

        val task = Task(
            id = 1L,
            title = request.title,
            description = request.description,
            owner = userEntity,
            collaborators = mutableSetOf(),
            labels = request.labels.toMutableSet(),
            board = board,
            status = TaskStatus.TODO,
            priority = TaskPriority.MID,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        every { userService.getUserByEmail(userEntity.email) } returns userEntity
        every { taskService.createTask(userEntity, request) } returns task

        val response = controller.createTask(request, userDetails)

        assertEquals(task.id, response.id)
        assertEquals(task.title, response.title)
        assertEquals(task.description, response.description)
        assertEquals(board.id, response.boardId)

        verify(exactly = 1) {
            taskService.createTask(userEntity, request)
        }
    }

    @Test
    fun `should return paginated tasks`() {
        val task = Task(
            id = 2L,
            title = "Another Task",
            description = "Another Description",
            owner = userEntity,
            collaborators = mutableSetOf(),
            labels = mutableSetOf("frontend"),
            board = board,
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        every { userService.getUserByEmail(userEntity.email) } returns userEntity
        every {
            taskService.getTasksWithFilters(
                user = userEntity,
                status = null,
                label = null,
                assigneeId = null,
                boardId = null,
                pageable = any()
            )
        } returns PageImpl(listOf(task))

        val result = controller.getTasks(
            user = userDetails,
            status = null,
            label = null,
            assigneeId = null,
            boardId = null,
            pageable = PageRequest.of(0, 10)
        )

        assertEquals(1, result.totalElements)
        assertEquals(task.id, result.content.first().id)
        assertEquals(board.id, result.content.first().boardId)

        verify(exactly = 1) {
            taskService.getTasksWithFilters(
                userEntity,
                null,
                null,
                null,
                null,
                any()
            )
        }
    }

    @Test
    fun `should throw ResourceNotFoundException when user not found`() {
        every { userService.getUserByEmail(userDetails.username) } returns null

        assertThrows<ResourceNotFoundException> {
            controller.getTasks(
                user = userDetails,
                status = null,
                label = null,
                assigneeId = null,
                boardId = null,
                pageable = PageRequest.of(0, 10)
            )
        }
    }
}
