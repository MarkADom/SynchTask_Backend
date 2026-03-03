package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.entity.BoardMember
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.project.domain.entity.Project
import com.synchtask.project.domain.entity.ProjectMember
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskMember
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import kotlin.collections.emptySet

class NotificationPoliciesTest {
    private val actor = User(
        id = 1L,
        name = "Actor",
        email = "actor@test.com",
        passwordHash = "hash"
    )
    private val owner = User(
        id = 2L,
        name = "Owner",
        email = "owner@test.com",
        passwordHash = "hash"
    )
    private val collab = User(
        id = 3L,
        name = "Collab",
        email = "collab@test.com",
        passwordHash = "hash"
    )

    @Test
    fun `task policies resolve recipients and defaults`() {
        val repo = mockk<TaskRepository>()
        val task = Task(
            id = 10L,
            title = "T",
            description = "D",
            owner = owner,
            board = Board(
                id = 5L,
                name = "B",
                owner = owner
            )
        )
        task.members.add(
            TaskMember(
                task = task,
                user = collab,
                role = MembershipRole.COLLABORATOR
            )
        )

        every { repo.findById(10L) } returns Optional.of(task)

        val assignedPolicy = TaskAssignedNotificationPolicy(repo)
        val statusPolicy = TaskStatusChangedNotificationPolicy(repo)
        val commentPolicy = TaskCommentNotificationPolicy(repo)

        val assignedActivity = Activity(
            actor = actor,
            type = ActivityType.TASK_ASSIGNED,
            referenceId = 10L
        )
        val statusActivity = Activity(
            actor = actor,
            type = ActivityType.TASK_STATUS_CHANGED,
            referenceId = 10L
        )
        val commentActivity = Activity(
            actor = actor,
            type = ActivityType.TASK_COMMENTED,
            referenceId = 10L
        )

        assertTrue(assignedPolicy.supports(assignedActivity))
        assertEquals(
            setOf("collab@test.com"), assignedPolicy.resolveRecipients(assignedActivity, null)
        )
        assertEquals("You were assigned to a task", assignedPolicy.buildMessage(assignedActivity))

        assertTrue(statusPolicy.supports(statusActivity))
        assertEquals(
            setOf("owner@test.com", "collab@test.com"), statusPolicy.resolveRecipients(statusActivity, null)
        )
        assertEquals("Task status updated", statusPolicy.buildMessage(statusActivity))

        assertTrue(commentPolicy.supports(commentActivity))
        assertEquals(
            setOf("owner@test.com", "collab@test.com"), commentPolicy.resolveRecipients(commentActivity, null)
        )
        assertEquals(NotificationType.TASK_UPDATE, commentPolicy.notificationType())
    }

    @Test
    fun `board and project policies use context snapshot on delete`() {
        val boardRepo = mockk<BoardRepository>()
        val projectRepo = mockk<ProjectRepository>()
        val boardPolicy = BoardNotificationPolicy(boardRepo)
        val projectPolicy = ProjectNotificationPolicy(projectRepo)

        val snapshot = ActivityContextSnapshot(
            ownerEmail = "owner@test.com",
            memberEmails = setOf("member@test.com"),
            collaboratorEmails = setOf("collab@test.com")
        )

        val boardDelete = Activity(
            actor = actor,
            type = ActivityType.BOARD_DELETED
        )
        val projectDelete = Activity(
            actor = actor,
            type = ActivityType.PROJECT_DELETED
        )

        assertEquals(
            setOf("owner@test.com", "collab@test.com"), boardPolicy.resolveRecipients(boardDelete, snapshot)
        )
        assertEquals(
            setOf("owner@test.com", "member@test.com"),
            projectPolicy.resolveRecipients(projectDelete, snapshot)
        )
    }

    @Test
    fun `board and project policies resolve update recipients from repositories`() {
        val boardRepo = mockk<BoardRepository>()
        val projectRepo = mockk<ProjectRepository>()

        val board = Board(
            id = 20L,
            name = "B",
            owner = owner
        )
        board.members.add(
            BoardMember(
                board = board,
                user = collab,
                role = MembershipRole.COLLABORATOR
            )
        )
        every { boardRepo.findById(20L) } returns Optional.of(board)

        val project = Project(
            id = 30L,
            name = "P",
            owner = owner,
            dueDate = LocalDate.now(),
            description = "D"
        )
        project.projectMembers.add(
            ProjectMember(
                project = project,
                user = collab,
                role = MembershipRole.COLLABORATOR,
                createdByUser = owner
            )
        )
        every { projectRepo.findById(30L) } returns Optional.of(project)

        val boardPolicy = BoardNotificationPolicy(boardRepo)
        val projectPolicy = ProjectNotificationPolicy(projectRepo)

        val boardUpdate = Activity(
            actor = actor,
            type = ActivityType.BOARD_UPDATED,
            referenceId = 20L
        )
        val projectUpdate = Activity(
            actor = actor,
            type = ActivityType.PROJECT_UPDATED,
            referenceId = 30L
        )

        assertEquals(
            setOf("owner@test.com", "collab@test.com"), boardPolicy.resolveRecipients(boardUpdate, null)
        )
        assertEquals(
            setOf("owner@test.com", "collab@test.com"), projectPolicy.resolveRecipients(projectUpdate, null)
        )
        assertEquals(NotificationType.GROUP, boardPolicy.notificationType())
        assertEquals(NotificationType.GROUP, projectPolicy.notificationType())
    }

    @Test
    fun `board and project policies cover supports and fallback branches`() {
        val boardPolicy = BoardNotificationPolicy(mockk<BoardRepository>())
        val projectPolicy = ProjectNotificationPolicy(mockk<ProjectRepository>())

        val unrelated = Activity(actor = actor, type = ActivityType.TASK_CREATED)
        assertFalse(boardPolicy.supports(unrelated))
        assertFalse(projectPolicy.supports(unrelated))

        val boardCreated = Activity(actor = owner, type = ActivityType.BOARD_CREATED)
        val projectCreated = Activity(actor = owner, type = ActivityType.PROJECT_CREATED)

        assertTrue(boardPolicy.supports(boardCreated))
        assertTrue(projectPolicy.supports(projectCreated))
        assertEquals("Board updated", boardPolicy.buildMessage(boardCreated))
        assertEquals("Project updated", projectPolicy.buildMessage(projectCreated))
        assertEquals(emptySet<String>(), boardPolicy.resolveRecipients(boardCreated, null))
        assertEquals(emptySet<String>(), projectPolicy.resolveRecipients(projectCreated, null))
    }
}
