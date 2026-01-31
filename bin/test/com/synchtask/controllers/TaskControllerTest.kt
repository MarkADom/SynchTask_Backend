package com.synchtask.controllers

import com.synchtask.dtos.task.TaskCreateDTO
import com.synchtask.entities.*
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.services.task.TaskService
import com.synchtask.services.user.UserService
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskControllerTest {

    private lateinit var taskService: TaskService
    private lateinit var userService: UserService
    private lateinit var controller: TaskController

    @BeforeEach
    fun setup() {
        taskService = mockk(relaxed = true)
        userService = mockk(relaxed = true)
        controller = TaskController(taskService, userService)
    }

    @Test
    fun `should create a task`() {
        val user = User(
            id = 1L,
            email = "admin@synchtask.com",
            name = "Admin",
            passwordHash = "hashed",
            role = UserRole.ADMIN
        )

        val request = TaskCreateDTO(
            title = "Test Task",
            description = "Test Description",
            assignees = listOf(2L),
            labels = listOf("urgent", "backend")
        )

        val userDetails: UserDetails = org.springframework.security.core.userdetails.User(
            user.email,
            user.passwordHash,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )


        val jwt = Jwt("token", null, null, mapOf("sub" to user.email), mapOf("sub" to user.email))
        val token = JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        every { userService.getUserByEmail(user.email) } returns user

        val task = Task(
            id = 10L,
            title = request.title,
            description = request.description,
            owner = user,
            collaborators = mutableSetOf(),
            labels = request.labels.toMutableSet(),
            createdAt = LocalDateTime.now()
        )

        every { taskService.createTask(user, request) } returns task

        val result = controller.createTask(request, token)

        assertEquals(task.id, result.id)
        assertEquals(task.title, result.title)
        assertEquals(task.description, result.description)
        assertEquals(task.owner.id, result.creatorId)
        assertEquals(task.labels.toList(), result.labels)
    }


    @Test
    fun `should get tasks for user`() {
        val userDetails: UserDetails = User("user@synchtask.com", "pass", listOf())
        val user = com.synchtask.entities.User(
            id = 1L,
            email = userDetails.username,
            name = "User",
            passwordHash = "hashed",
            role = UserRole.USER
        )

        val task1 = Task(
            id = 1L,
            title = "Task 1",
            description = "Description 1",
            owner = user,
            collaborators = mutableSetOf(user),
            labels = mutableSetOf("frontend"),
            createdAt = LocalDateTime.now()
        )

        every { userService.getUserByEmail(userDetails.username) } returns user
        every { taskService.getTasksForUser(user) } returns listOf(task1)

        val result = controller.getTasks(userDetails)

        assertEquals(1, result.size)
        assertEquals(task1.id, result[0].id)
        assertEquals("frontend", result[0].labels.first())
    }

    @Test
    fun `should update a task`() {
        val user = com.synchtask.entities.User(
            id = 1L,
            email = "admin@synchtask.com",
            name = "Admin",
            passwordHash = "hashed",
            role = UserRole.ADMIN
        )

        val updatedRequest = TaskCreateDTO(
            title = "Updated Task",
            description = "Updated Desc",
            assignees = emptyList(),
            labels = listOf("urgent", "updated")
        )

        val jwt = Jwt("token", null, null, mapOf("sub" to user.email), mapOf("sub" to user.email))
        val token = JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        val originalTask = Task(
            id = 123L,
            title = "Old Title",
            description = "Old Description",
            owner = user,
            collaborators = mutableSetOf(),
            labels = mutableSetOf("bug"),
            createdAt = LocalDateTime.now()
        )

        every { userService.getUserByEmail(user.email) } returns user
        every { taskService.findTaskById(123L) } returns originalTask
        every { taskService.updateTask(123L, updatedRequest) } returns originalTask.copy(
            title = updatedRequest.title,
            description = updatedRequest.description,
            labels = updatedRequest.labels.toMutableSet(),
            updatedAt = LocalDateTime.now()
        )

        val result = controller.updateTask(123L, updatedRequest, token)

        assertEquals("Updated Task", result.title)
        assertEquals("Updated Desc", result.description)
        assertEquals(listOf("urgent", "updated"), result.labels)
    }

    @Test
    fun `should throw UnauthorizedAccessException if user not owner or admin`() {
        val user = com.synchtask.entities.User(
            id = 1L,
            email = "user@synchtask.com",
            name = "Regular User",
            passwordHash = "hashed",
            role = UserRole.USER
        )

        val taskOwner = com.synchtask.entities.User(
            id = 2L,
            email = "other@synchtask.com",
            name = "Owner",
            passwordHash = "hashed",
            role = UserRole.OWNER
        )

        val jwt = Jwt("token", null, null, mapOf("sub" to user.email), mapOf("sub" to user.email))
        val token = JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")))

        val task = Task(
            id = 50L,
            title = "Forbidden Task",
            description = "Can't update this",
            owner = taskOwner,
            collaborators = mutableSetOf(),
            labels = mutableSetOf(),
            createdAt = LocalDateTime.now()
        )

        every { userService.getUserByEmail(user.email) } returns user
        every { taskService.findTaskById(task.id!!) } returns task

        val exception = assertThrows<UnauthorizedAccessException> {
            controller.updateTask(task.id!!, TaskCreateDTO("x", "x", listOf(), listOf()), token)
        }

        assertTrue(exception.message!!.contains("not authorized"))
    }


    @Test
    fun `should assign collaborator to task`() {
        every { taskService.assignCollaborator(42L, "collab@synchtask.com") } just Runs

        val response = controller.assignCollaborator(42L, "collab@synchtask.com")

        assertEquals(ResponseEntity.ok("Collaborator assigned successfully"), response)
        verify(exactly = 1) { taskService.assignCollaborator(42L, "collab@synchtask.com") }
    }

    @Test
    fun `should throw UnauthorizedAccessException if user not found`() {
        val email = "ghost@synchtask.com"
        val request = TaskCreateDTO("title", "desc", listOf(), listOf())

        val jwt = Jwt("token", null, null, mapOf("sub" to email), mapOf("sub" to email))
        val token = JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        every { userService.getUserByEmail(email) } returns null

        val exception = assertThrows<UnauthorizedAccessException> {
            controller.createTask(request, token)
        }

        assertTrue(exception.message!!.contains("User not found or unauthorized"))
    }

    @Test
    fun `should throw UnauthorizedAccessException when userDetails is null`() {
        val exception = assertThrows<UnauthorizedAccessException> {
            controller.getTasks(null)
        }

        assertEquals("Authentication failed: userDetails is null", exception.message)
    }

    @Test
    fun `should throw UnauthorizedAccessException when user is not OWNER or ADMIN`() {
        val user = com.synchtask.entities.User(
            id = 1L,
            email = "user@synchtask.com",
            name = "User",
            passwordHash = "hash",
            role = UserRole.USER
        )

        val request = TaskCreateDTO("Title", "Desc", listOf(), listOf())
        val jwt = Jwt("token", null, null, mapOf("sub" to user.email), mapOf("sub" to user.email))
        val token = JwtAuthenticationToken(jwt)

        every { userService.getUserByEmail(user.email) } returns user

        val exception = assertThrows<UnauthorizedAccessException> {
            controller.createTask(request, token)
        }

        assertEquals("User ${user.email} attempted to create a task but lacks permissions.", exception.message)
    }

    @Test
    fun `should throw ResourceNotFoundException when user not found for getTasks`() {
        val userDetails: UserDetails = User("missing@synchtask.com", "pass", listOf())

        every { userService.getUserByEmail(userDetails.username) } returns null

        val exception = assertThrows<ResourceNotFoundException> {
            controller.getTasks(userDetails)
        }

        assertEquals("User not found: ${userDetails.username}", exception.message)
    }

    @Test
    fun `should throw ResourceNotFoundException when user not found for updateTask`() {
        val email = "notfound@synchtask.com"
        val jwt = Jwt("token", null, null, mapOf("sub" to email), mapOf("sub" to email))
        val token = JwtAuthenticationToken(jwt)

        every { userService.getUserByEmail(email) } returns null

        val exception = assertThrows<ResourceNotFoundException> {
            controller.updateTask(1L, TaskCreateDTO("x", "y", listOf(), listOf()), token)
        }

        assertEquals("User not found.", exception.message)
    }

    @Test
    fun `should throw UnauthorizedAccessException when user is not owner or admin`() {
        val owner = com.synchtask.entities.User(
            id = 1L,
            email = "owner@synchtask.com",
            name = "Owner",
            passwordHash = "x",
            role = UserRole.OWNER
        )

        val intruder = com.synchtask.entities.User(
            id = 2L,
            email = "intruder@synchtask.com",
            name = "Hacker",
            passwordHash = "y",
            role = UserRole.USER
        )

        val jwt = Jwt("token", null, null, mapOf("sub" to intruder.email), mapOf("sub" to intruder.email))
        val token = JwtAuthenticationToken(jwt)

        val task = Task(
            id = 1L,
            title = "T",
            description = "D",
            owner = owner,
            collaborators = mutableSetOf(),
            labels = mutableSetOf(),
            createdAt = LocalDateTime.now()
        )

        every { userService.getUserByEmail(intruder.email) } returns intruder
        every { taskService.findTaskById(1L) } returns task

        val exception = assertThrows<UnauthorizedAccessException> {
            controller.updateTask(1L, TaskCreateDTO("New", "New Desc", listOf(), listOf()), token)
        }

        assertEquals("User ${intruder.email} is not authorized to update this task.", exception.message)
    }

}
