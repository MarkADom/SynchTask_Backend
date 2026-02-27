package com.synchtask.task.presentation.controller

import com.synchtask.board.domain.entity.Board
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.application.dto.TaskAssigneeUpdateDTO
import com.synchtask.task.application.dto.TaskCreateDTO
import com.synchtask.task.application.dto.TaskLabelUpdateDTO
import com.synchtask.task.application.dto.TaskUpdateDTO
import com.synchtask.task.application.service.TaskService
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskMember
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.user.application.service.AuthenticatedUserService
import com.synchtask.user.domain.entity.UserRole
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
    private lateinit var authenticatedUserService: AuthenticatedUserService
    private lateinit var controller: TaskController

    private lateinit var userEntity: com.synchtask.user.domain.entity.User
    private lateinit var userDetails: UserDetails
    private lateinit var board: Board

    @BeforeEach
    fun setup() {
        taskService = mockk()
        authenticatedUserService = mockk()
        controller = TaskController(taskService, authenticatedUserService)

        userEntity =
            com.synchtask.user.domain.entity.User(
                id = 1L,
                email = "user@synchtask.com",
                name = "User",
                passwordHash = "hash",
                role = UserRole.USER
            )

        userDetails =
            User(
                userEntity.email,
                "hash",
                emptyList()
            )

        board =
            Board(
                id = 10L,
                name = "Main Board",
                owner = userEntity,
                project = null
            )
    }

    private fun newTask(id: Long, title: String) = Task(
        id = id,
        title = title,
        description = "Desc",
        owner = userEntity,
        collaborators = mutableSetOf(),
        labels = mutableSetOf(),
        board = board,
        status = TaskStatus.TODO,
        priority = TaskPriority.MID,
        createdAt = LocalDateTime.now(),
        updatedAt = null
    )

    @Test
    fun `should create task successfully`() {
        val request =
            TaskCreateDTO(
                title = "Test Task",
                description = "Test Description",
                assignees = emptyList(),
                labels = listOf("backend"),
                boardId = board.id!!
            )

        val task = newTask(1L, request.title)

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { taskService.createTask(userEntity, request) } returns task

        val response = controller.createTask(request, userDetails)

        assertEquals(task.id, response.id)
        assertEquals(task.title, response.title)
        assertEquals(board.id, response.boardId)
    }

    @Test
    fun `should return paginated task list items`() {
        val task = newTask(2L, "Another Task")

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
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

        val result =
            controller.getTasks(
                userDetails,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
            )

        assertEquals(1, result.totalElements)
        assertEquals(task.id, result.content.first().id)
        assertEquals(task.title, result.content.first().title)
    }

    @Test
    fun `should throw ResourceNotFoundException when user not found on list`() {
        every { authenticatedUserService.requireUser(userDetails) } throws
            ResourceNotFoundException("Authenticated user not found: ${userDetails.username}")

        assertThrows<ResourceNotFoundException> {
            controller.getTasks(
                userDetails,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
            )
        }
    }

    @Test
    fun `should return task detail when user has access`() {
        val task = newTask(5L, "Detail Task")
        task.members.add(
            TaskMember(
                task = task,
                user = userEntity,
                role = MembershipRole.COLLABORATOR
            )
        )

        every { taskService.findTaskById(5L) } returns task
        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        val result = controller.getTaskDetail(5L, userDetails)

        assertEquals(task.id, result.id)
    }

    @Test
    fun `should throw UnauthorizedAccessException when user cannot access task`() {
        val otherOwner =
            com.synchtask.user.domain.entity.User(
                id = 99L,
                email = "other@test.com",
                name = userEntity.name,
                passwordHash = userEntity.passwordHash,
                profilePictureUrl = userEntity.profilePictureUrl,
                role = userEntity.role,
                createdAt = userEntity.createdAt,
                lastLogin = userEntity.lastLogin,
                lastActivity = userEntity.lastActivity,
                isActive = userEntity.isActive,
                isOnline = userEntity.isOnline,
                onboardingNotified = userEntity.onboardingNotified
            )

        val base = newTask(5L, "Detail Task")
        val task =
            Task(
                id = base.id,
                title = base.title,
                description = base.description,
                owner = otherOwner,
                collaborators = base.collaborators,
                labels = base.labels,
                status = base.status,
                priority = base.priority,
                comments = base.comments,
                createdAt = base.createdAt,
                updatedAt = base.updatedAt,
                board = base.board
            )

        every { taskService.findTaskById(5L) } returns task
        every { authenticatedUserService.requireUser(userDetails) } returns userEntity

        assertThrows<UnauthorizedAccessException> {
            controller.getTaskDetail(5L, userDetails)
        }
    }

    @Test
    fun `should update task successfully`() {
        val dto =
            TaskUpdateDTO(
                title = "Updated",
                description = "Updated Desc",
                status = TaskStatus.COMPLETED,
                priority = null
            )

        val updatedTask = newTask(1L, "Updated")

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { taskService.updateTask(1L, dto, userEntity) } returns updatedTask

        val response = controller.updateTask(1L, dto, userDetails)

        assertEquals(updatedTask.id, response.body?.id)
    }

    @Test
    fun `should delete task successfully`() {
        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { taskService.deleteTask(1L, userEntity) } just Runs

        val response = controller.deleteTask(1L, userDetails)

        assertEquals(ApiMessageResponseDTO("Task deleted successfully"), response.body)
    }

    @Test
    fun `should assign collaborator successfully`() {
        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { taskService.assignCollaborator(1L, "collab@test.com", userEntity) } just Runs

        val response = controller.assignCollaborator(1L, "collab@test.com", userDetails)

        assertEquals(ApiMessageResponseDTO("Collaborator assigned successfully"), response.body)
    }

    @Test
    fun `should update task labels successfully`() {
        val dto = TaskLabelUpdateDTO(labels = listOf("urgent"))

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { taskService.updateTaskLabels(1L, dto.labels, userEntity) } just Runs

        val response = controller.updateLabels(1L, dto, userDetails)

        assertEquals(ApiMessageResponseDTO("Labels updated successfully"), response.body)
    }

    @Test
    fun `should update task assignees successfully`() {
        val dto = TaskAssigneeUpdateDTO(userIds = listOf(2L, 3L))

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { taskService.updateTaskAssignees(1L, dto.userIds, userEntity) } just Runs

        val response = controller.updateAssignees(1L, dto, userDetails)

        assertEquals(ApiMessageResponseDTO("Assignees updated successfully"), response.body)
    }
}
