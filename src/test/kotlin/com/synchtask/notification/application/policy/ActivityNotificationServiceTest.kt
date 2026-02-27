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

class ActivityNotificationServiceTest {
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
    fun `board policy should resolve recipients from board membership`() {
        val boardRepository = mockk<BoardRepository>()
        val policy = BoardNotificationPolicy(boardRepository)
        val board = Board(id = 10L, name = "Board", owner = owner)
        board.members.add(
            BoardMember(
                board = board,
                user = collab,
                role = MembershipRole.COLLABORATOR
            )
        )
        board.members.add(
            BoardMember(
                board = board,
                user = actor,
                role = MembershipRole.COLLABORATOR
            )
        )

        val updated =
            Activity(
                actor = actor,
                type = ActivityType.BOARD_UPDATED,
                referenceId = 10L,
                description = "updated"
            )
        val deleted = Activity(
            actor = actor,
            type = ActivityType.BOARD_DELETED,
            referenceId = 10L
        )

        every { boardRepository.findById(10L) } returns Optional.of(board)

        assertTrue(policy.supports(updated))
        assertFalse(policy.supports(Activity(actor = actor, type = ActivityType.TASK_COMMENTED)))
        assertEquals(setOf(owner.email, collab.email), policy.resolveRecipients(updated))
        assertEquals(NotificationType.GROUP, policy.notificationType())

        val recipientsFromSnapshot = policy.resolveRecipients(
            deleted,
            ActivityContextSnapshot(ownerEmail = owner.email, collaboratorEmails = setOf(collab.email, actor.email))
        )
        assertEquals(setOf(owner.email, collab.email), recipientsFromSnapshot)
    }

    @Test
    fun `project policy should resolve recipients from project membership`() {
        val projectRepository = mockk<ProjectRepository>()
        val policy = ProjectNotificationPolicy(projectRepository)

        val project = Project(
            id = 7L,
            name = "Project",
            owner = owner,
            dueDate = LocalDate.now()
        )
        project.projectMembers.add(
            ProjectMember(
                project = project,
                user = collab,
                role = MembershipRole.COLLABORATOR
            )
        )
        project.projectMembers.add(
            ProjectMember(
                project = project,
                user = actor,
                role = MembershipRole.COLLABORATOR
            )
        )

        val created = Activity(
            actor = actor,
            type = ActivityType.PROJECT_CREATED,
            referenceId = 7L
        )
        val updated = Activity(
            actor = actor,
            type = ActivityType.PROJECT_UPDATED,
            referenceId = 7L
        )

        every { projectRepository.findById(7L) } returns Optional.of(project)

        assertTrue(policy.supports(created))
        assertEquals(setOf(owner.email), policy.resolveRecipients(created))
        assertEquals(setOf(owner.email, collab.email), policy.resolveRecipients(updated))
    }

    @Test
    fun `task policies should resolve recipients from task membership`() {
        val taskRepository = mockk<TaskRepository>()
        val statusPolicy = TaskStatusChangedNotificationPolicy(taskRepository)
        val commentPolicy = TaskCommentNotificationPolicy(taskRepository)
        val assignedPolicy = TaskAssignedNotificationPolicy(taskRepository)

        val board = Board(id = 11L, name = "Board", owner = owner)
        val task = Task(
            id = 101L,
            title = "Task",
            description = "Desc",
            owner = owner,
            status = TaskStatus.TODO,
            priority = TaskPriority.MID,
            board = board
        )
        task.members.add(
            TaskMember(
                task = task,
                user = collab,
                role = MembershipRole.COLLABORATOR
            )
        )
        task.members.add(
            TaskMember(
                task = task,
                user = actor,
                role = MembershipRole.COLLABORATOR
            )
        )

        every { taskRepository.findById(101L) } returns Optional.of(task)

        val statusActivity = Activity(
            actor = actor,
            type = ActivityType.TASK_STATUS_CHANGED,
            referenceId = 101L
        )
        val commentActivity = Activity(
            actor = actor,
            type = ActivityType.TASK_COMMENTED,
            referenceId = 101L
        )
        val assignActivity = Activity(
            actor = actor,
            type = ActivityType.TASK_ASSIGNED,
            referenceId = 101L
        )

        assertEquals(setOf(owner.email, collab.email), statusPolicy.resolveRecipients(statusActivity))
        assertEquals(setOf(owner.email, collab.email), commentPolicy.resolveRecipients(commentActivity))
        assertEquals(setOf(collab.email), assignedPolicy.resolveRecipients(assignActivity))
    }
}
