package com.synchtask.services.task

import com.synchtask.dtos.task.TaskCreateDTO
import com.synchtask.dtos.task.TaskResponseDTO
import com.synchtask.entities.NotificationType
import com.synchtask.entities.Task
import com.synchtask.entities.TaskStatus
import com.synchtask.entities.User
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.TaskRepository
import com.synchtask.repositories.UserRepository
import com.synchtask.services.notification.NotificationService
import com.synchtask.websocket.TaskWebSocketService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskServiceTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationService: NotificationService
    private lateinit var taskWebSocketService: TaskWebSocketService
    private lateinit var taskService: TaskService

    private val owner = User(id = 1L, name = "Owner", email = "owner@example.com", passwordHash = "hash")
    private val collaborator = User(id = 2L, name = "Collab", email = "collab@example.com", passwordHash = "hash")
    private val task = Task(id = 10L, title = "Old Title", description = "Old Desc", owner = owner)

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        userRepository = mockk()
        notificationService = mockk(relaxed = true)
        taskWebSocketService = mockk(relaxed = true)

        taskService = TaskService(
            taskRepository,
            userRepository,
            notificationService,
            taskWebSocketService
        )
    }

    @Test
    fun `should create task and return saved entity`() {
        val request = TaskCreateDTO("New Title", "New Desc", emptyList(), emptyList())
        every { taskRepository.save(any()) } answers { firstArg() }

        val result = taskService.createTask(owner, request)

        assertEquals("New Title", result.title)
        assertEquals(owner, result.owner)
        verify { taskRepository.save(any()) }
    }

    @Test
    fun `should throw when task not found`() {
        every { taskRepository.findById(any()) } returns Optional.empty()
        assertFailsWith<ResourceNotFoundException> {
            taskService.findTaskById(999L)
        }
    }

    @Test
    fun `should assign collaborator and send notification`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)
        every { taskRepository.save(any()) } returns task

        taskService.assignCollaborator(task.id!!, collaborator.email)

        assert(task.collaborators.contains(collaborator))
        verify { notificationService.sendNotification(collaborator.email, any(), NotificationType.TASK_UPDATE, task.id) }
    }

    @Test
    fun `should skip reassigning existing collaborator`() {
        task.collaborators.add(collaborator)
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { userRepository.findByEmail(collaborator.email) } returns Optional.of(collaborator)

        taskService.assignCollaborator(task.id!!, collaborator.email)

        verify(exactly = 0) { taskRepository.save(any()) }
        verify(exactly = 0) { notificationService.sendNotification(any(), any(), any(), any()) }
    }

    @Test
    fun `should get tasks for a user`() {
        every { taskRepository.findTasksByUser(owner) } returns listOf(task)
        val result = taskService.getTasksForUser(owner)
        assertEquals(1, result.size)
    }

    @Test
    fun `should update task status and notify collaborators`() {
        task.collaborators.add(collaborator)
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskRepository.save(any()) } returns task

        taskService.updateTaskStatus(task.id!!, TaskStatus.IN_PROGRESS)

        assertEquals(TaskStatus.IN_PROGRESS, task.status)
        verify { notificationService.sendNotification(collaborator.email, any(), NotificationType.TASK_UPDATE, task.id) }
        verify { taskWebSocketService.sendTaskUpdate(any<TaskResponseDTO>()) }
    }

    @Test
    fun `should not update task status if already set`() {
        task.status = TaskStatus.IN_PROGRESS
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        taskService.updateTaskStatus(task.id!!, TaskStatus.IN_PROGRESS)

        verify(exactly = 0) { taskRepository.save(any()) }
        verify(exactly = 0) { taskWebSocketService.sendTaskUpdate(any()) }
    }

    @Test
    fun `should update task fields and save`() {
        val update = TaskCreateDTO("Updated", "Updated Desc", emptyList(), listOf("Urgent"))
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskRepository.save(any()) } returns task

        val updated = taskService.updateTask(task.id!!, update)

        assertEquals("Updated", updated.title)
        assertEquals("Updated Desc", updated.description)
        assertEquals(setOf("Urgent"), updated.labels)
        verify { taskRepository.save(any()) }
    }
}
