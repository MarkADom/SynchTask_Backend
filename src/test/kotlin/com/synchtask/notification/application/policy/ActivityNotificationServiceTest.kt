package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.project.domain.entity.Project
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPoliciesTest {
    private val actor = User(id = 1L, name = "Actor", email = "actor@test.com", passwordHash = "hash")
    private val owner = User(id = 2L, name = "Owner", email = "owner@test.com", passwordHash = "hash")
    private val collab = User(id = 3L, name = "Collab", email = "collab@test.com", passwordHash = "hash")

    @Test
    fun `board policy should support board activity types and resolve recipients`() {
        val boardRepository = mockk<BoardRepository>()
        val policy = BoardNotificationPolicy(boardRepository)
        val board = Board(id = 10L, name = "Board", owner = owner, collaborators = mutableSetOf(collab, actor))
        val updated =
            Activity(actor = actor, type = ActivityType.BOARD_UPDATED, referenceId = 10L, description = "updated")
        val deleted = Activity(actor = actor, type = ActivityType.BOARD_DELETED, referenceId = 10L)

        every { boardRepository.findById(10L) } returns Optional.of(board)

        assertTrue(policy.supports(updated))
        assertFalse(policy.supports(Activity(actor = actor, type = ActivityType.TASK_COMMENTED)))
        assertEquals(setOf(owner.email, collab.email), policy.resolveRecipients(updated))
        assertEquals(NotificationType.GROUP, policy.notificationType())
        assertEquals("updated", policy.buildMessage(updated))

        val recipientsFromSnapshot =
            policy.resolveRecipients(
                deleted,
                ActivityContextSnapshot(ownerEmail = owner.email, collaboratorEmails = setOf(collab.email, actor.email))
            )
        assertEquals(setOf(owner.email, collab.email), recipientsFromSnapshot)
    }

    @Test
    fun `project policy should support project events and resolve recipients`() {
        val projectRepository = mockk<ProjectRepository>()
        val policy = ProjectNotificationPolicy(projectRepository)

        val project =
            Project(
                id = 7L,
                name = "Project",
                owner = owner,
                dueDate = LocalDate.now(),
                members = mutableSetOf(collab, actor)
            )

        val created = Activity(actor = actor, type = ActivityType.PROJECT_CREATED, referenceId = 7L)
        val updated =
            Activity(
                actor = actor,
                type = ActivityType.PROJECT_UPDATED,
                referenceId = 7L,
                description = "project changed"
            )
        val deleted = Activity(actor = actor, type = ActivityType.PROJECT_DELETED)

        every { projectRepository.findById(7L) } returns Optional.of(project)

        assertTrue(policy.supports(created))
        assertFalse(policy.supports(Activity(actor = actor, type = ActivityType.BOARD_UPDATED)))
        assertEquals(setOf(owner.email), policy.resolveRecipients(created))
        assertEquals(setOf(owner.email, collab.email), policy.resolveRecipients(updated))
        assertEquals(NotificationType.GROUP, policy.notificationType())
        assertEquals("project changed", policy.buildMessage(updated))

        val recipientsFromSnapshot =
            policy.resolveRecipients(
                deleted,
                ActivityContextSnapshot(ownerEmail = owner.email, memberEmails = setOf(collab.email, actor.email))
            )
        assertEquals(setOf(owner.email, collab.email), recipientsFromSnapshot)
    }

    @Test
    fun `task related policies should support types and resolve recipients`() {
        val taskRepository = mockk<TaskRepository>()
        val statusPolicy = TaskStatusChangedNotificationPolicy(taskRepository)
        val commentPolicy = TaskCommentNotificationPolicy(taskRepository)
        val assignedPolicy = TaskAssignedNotificationPolicy(taskRepository)

        val board = Board(id = 11L, name = "Board", owner = owner)
        val task =
            Task(
                id = 101L,
                title = "Task",
                description = "Desc",
                owner = owner,
                collaborators = mutableSetOf(collab, actor),
                status = TaskStatus.TODO,
                priority = TaskPriority.MID,
                board = board
            )

        every { taskRepository.findById(101L) } returns Optional.of(task)

        val statusActivity =
            Activity(actor = actor, type = ActivityType.TASK_STATUS_CHANGED, referenceId = 101L, description = "status")
        val commentActivity = Activity(actor = actor, type = ActivityType.TASK_COMMENTED, referenceId = 101L)
        val assignActivity = Activity(actor = actor, type = ActivityType.TASK_ASSIGNED, referenceId = 101L)

        assertTrue(statusPolicy.supports(statusActivity))
        assertTrue(commentPolicy.supports(commentActivity))
        assertTrue(assignedPolicy.supports(assignActivity))
        assertFalse(assignedPolicy.supports(Activity(actor = actor, type = ActivityType.PROJECT_CREATED)))

        assertEquals(setOf(owner.email, collab.email), statusPolicy.resolveRecipients(statusActivity))
        assertEquals(setOf(owner.email, collab.email), commentPolicy.resolveRecipients(commentActivity))
        assertEquals(setOf(collab.email), assignedPolicy.resolveRecipients(assignActivity))

        assertEquals(NotificationType.TASK_UPDATE, statusPolicy.notificationType())
        assertEquals(NotificationType.TASK_UPDATE, commentPolicy.notificationType())
        assertEquals(NotificationType.TASK_UPDATE, assignedPolicy.notificationType())
        assertEquals("status", statusPolicy.buildMessage(statusActivity))
        assertEquals("New comment on a task", commentPolicy.buildMessage(commentActivity))
        assertEquals("You were assigned to a task", assignedPolicy.buildMessage(assignActivity))
    }
}
