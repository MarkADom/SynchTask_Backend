package com.synchtask.mappers

import com.synchtask.entities.*
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.presentation.mapper.TaskMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TaskMapperTest {

    private val now = LocalDateTime.now()

    private val owner = User(
        id = 1L,
        name = "Owner User",
        email = "owner@example.com",
        passwordHash = "hash"
    )

    private val collaborator = User(
        id = 2L,
        name = "Collaborator",
        email = "collab@example.com",
        passwordHash = "hash"
    )

    private val project = Project(
        id = 10L,
        name = "Test Project",
        description = "Project description",
        owner = owner,
        dueDate = LocalDate.now().plusDays(30)
    )

    private val board = Board(
        id = 20L,
        name = "Main Board",
        owner = owner,
        project = project
    )

    private val task = Task(
        id = 100L,
        title = "Test Task",
        description = "This is a test task",
        owner = owner,
        collaborators = mutableSetOf(collaborator),
        status = TaskStatus.TODO,
        priority = TaskPriority.MID,
        labels = mutableSetOf("urgent", "backend"),
        createdAt = now,
        updatedAt = now,
        board = board
    )

    @Test
    fun `should map Task to TaskResponseDTO correctly`() {
        val dto = TaskMapper.toTaskResponseDTO(task)

        assertEquals(100L, dto.id)
        assertEquals("Test Task", dto.title)
        assertEquals("This is a test task", dto.description)

        assertEquals(1L, dto.creatorId)
        assertEquals("Owner User", dto.creatorName)

        assertEquals(listOf(2L), dto.assignees)

        assertEquals(TaskStatus.TODO, dto.status)
        assertEquals(TaskPriority.MID, dto.priority)

        assertEquals(setOf("urgent", "backend"), dto.labels.toSet())

        assertEquals(now, dto.createdAt)
        assertEquals(now, dto.updatedAt)

        assertEquals(20L, dto.boardId)
        assertEquals("Main Board", dto.boardName)
        assertEquals("Test Project", dto.projectName)
    }

    @Test
    fun `should map TaskComment to TaskCommentResponseDTO correctly`() {
        val comment = TaskComment(
            id = 500L,
            task = task,
            user = collaborator,
            content = "Looks good!",
            createdAt = now
        )

        val dto = TaskMapper.toTaskCommentResponseDTO(comment)

        assertEquals(500L, dto.id)
        assertEquals(100L, dto.taskId)
        assertEquals(2L, dto.userId)
        assertEquals("Looks good!", dto.content)
        assertEquals(now, dto.createdAt)
    }

    @Test
    fun `should throw when Task id is null`() {
        val invalidTask = task.copy(id = null)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            TaskMapper.toTaskResponseDTO(invalidTask)
        }

        assertEquals("Task ID cannot be null", ex.message)
    }

    @Test
    fun `should throw when TaskComment ids are null`() {
        val comment = TaskComment(
            id = null,
            task = task.copy(id = null),
            user = collaborator.copy(id = null),
            content = "Missing IDs",
            createdAt = now
        )

        assertThrows(NullPointerException::class.java) {
            TaskMapper.toTaskCommentResponseDTO(comment)
        }
    }
}
