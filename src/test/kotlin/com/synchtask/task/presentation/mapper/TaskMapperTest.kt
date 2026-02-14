package com.synchtask.task.presentation.mapper

import com.synchtask.board.domain.entity.Board
import com.synchtask.project.domain.entity.Project
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskComment
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.user.domain.entity.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TaskMapperTest {
    private val now = LocalDateTime.now()

    private val owner =
        User(
            id = 1L,
            name = "Owner User",
            email = "owner@example.com",
            passwordHash = "hash"
        )

    private val collaborator =
        User(
            id = 2L,
            name = "Collaborator",
            email = "collab@example.com",
            passwordHash = "hash"
        )

    private val project =
        Project(
            id = 10L,
            name = "Test Project",
            description = "Project description",
            owner = owner,
            dueDate = LocalDate.now().plusDays(30)
        )

    private val board =
        Board(
            id = 20L,
            name = "Main Board",
            owner = owner,
            project = project
        )

    private val task =
        Task(
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
        val dto = TaskMapper.toResponse(task)

        Assertions.assertEquals(100L, dto.id)
        Assertions.assertEquals("Test Task", dto.title)
        Assertions.assertEquals("This is a test task", dto.description)

        Assertions.assertEquals(1L, dto.creatorId)
        Assertions.assertEquals("Owner User", dto.creatorName)

        Assertions.assertEquals(listOf(2L), dto.assignees)

        Assertions.assertEquals(TaskStatus.TODO, dto.status)
        Assertions.assertEquals(TaskPriority.MID, dto.priority)

        Assertions.assertEquals(setOf("urgent", "backend"), dto.labels.toSet())

        Assertions.assertEquals(now, dto.createdAt)
        Assertions.assertEquals(now, dto.updatedAt)

        Assertions.assertEquals(20L, dto.boardId)
        Assertions.assertEquals("Main Board", dto.boardName)
        Assertions.assertEquals("Test Project", dto.projectName)
    }

    @Test
    fun `should map TaskComment to TaskCommentResponseDTO correctly`() {
        val comment =
            TaskComment(
                id = 500L,
                task = task,
                user = collaborator,
                content = "Looks good!",
                createdAt = now
            )

        val dto = TaskMapper.toCommentResponse(comment)

        Assertions.assertEquals(500L, dto.id)
        Assertions.assertEquals(100L, dto.taskId)
        Assertions.assertEquals(2L, dto.userId)
        Assertions.assertEquals("Looks good!", dto.content)
        Assertions.assertEquals(now, dto.createdAt)
    }

    @Test
    fun `should throw when Task id is null`() {
        val invalidTask =
            Task(
                id = null,
                title = task.title,
                description = task.description,
                owner = task.owner,
                collaborators = task.collaborators,
                labels = task.labels,
                status = task.status,
                priority = task.priority,
                comments = task.comments,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                board = task.board
            )

        val ex =
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                TaskMapper.toResponse(invalidTask)
            }

        Assertions.assertEquals("Task ID cannot be null", ex.message)
    }

    @Test
    fun `should throw when TaskComment ids are null`() {
        val comment =
            TaskComment(
                id = null,
                task =
                Task(
                    id = null,
                    title = task.title,
                    description = task.description,
                    owner = task.owner,
                    collaborators = task.collaborators,
                    labels = task.labels,
                    status = task.status,
                    priority = task.priority,
                    comments = task.comments,
                    createdAt = task.createdAt,
                    updatedAt = task.updatedAt,
                    board = task.board
                ),
                user =
                User(
                    id = null,
                    name = collaborator.name,
                    email = collaborator.email,
                    passwordHash = collaborator.passwordHash,
                    profilePictureUrl = collaborator.profilePictureUrl,
                    role = collaborator.role,
                    createdAt = collaborator.createdAt,
                    lastLogin = collaborator.lastLogin,
                    lastActivity = collaborator.lastActivity,
                    isActive = collaborator.isActive,
                    isOnline = collaborator.isOnline,
                    onboardingNotified = collaborator.onboardingNotified
                ),
                content = "Missing IDs",
                createdAt = now
            )

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            TaskMapper.toCommentResponse(comment)
        }
    }
}
