package com.synchtask.task.domain.entity

import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.entity.BoardMember
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.task.domain.exception.InvalidTaskStatusTransitionException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskEntityTest {
    private fun user(id: Long, role: UserRole = UserRole.USER): User =
        User(
            id = id,
            name = "User$id",
            email = "user$id@test.com",
            passwordHash = "hash",
            role = role
        )

    private fun board(owner: User) =
        Board(
            id = 20L,
            name = "Board",
            owner = owner
        )

    private fun task(owner: User, board: Board) =
        Task(
            id = 10L,
            title = "Task",
            description = "Desc",
            owner = owner,
            board = board,
            status = TaskStatus.TODO,
            priority = TaskPriority.MID
        )

    @Test
    fun `canBeEditedBy should allow membership and fallback owner collaborator`() {
        val owner = user(1L)
        val taskMemberUser = user(2L)
        val legacyCollaborator = user(3L)
        val outsider = user(4L)
        val task = task(owner, board(owner))

        task.members.add(
            TaskMember(
                task = task,
                user = taskMemberUser,
                role = MembershipRole.COLLABORATOR
            )
        )
        task.collaborators.add(legacyCollaborator)

        assertTrue(task.canBeEditedBy(owner))
        assertTrue(task.canBeEditedBy(taskMemberUser))
        assertTrue(task.canBeEditedBy(legacyCollaborator))
        assertFalse(task.canBeEditedBy(outsider))
    }

    @Test
    fun `canBeAccessedBy should allow task membership board membership and fallback`() {
        val owner = user(1L)
        val boardOwner = user(2L)
        val taskMemberUser = user(3L)
        val boardMemberUser = user(4L)
        val boardLegacyCollaborator = user(5L)
        val outsider = user(6L)
        val board = board(boardOwner)
        val task = task(owner, board)

        task.members.add(
            TaskMember(
                task = task,
                user = taskMemberUser,
                role = MembershipRole.COLLABORATOR
            )
        )
        board.members.add(
            BoardMember(
                board = board,
                user = boardMemberUser,
                role = MembershipRole.COLLABORATOR
            )
        )
        board.collaborators.add(boardLegacyCollaborator)

        assertTrue(task.canBeAccessedBy(taskMemberUser))
        assertTrue(task.canBeAccessedBy(boardMemberUser))
        assertTrue(task.canBeAccessedBy(boardLegacyCollaborator))
        assertFalse(task.canBeAccessedBy(outsider))
    }

    @Test
    fun `admin should always edit and access task`() {
        val owner = user(1L)
        val admin = user(99L, UserRole.ADMIN)
        val task = task(owner, board(owner))

        assertTrue(task.canBeEditedBy(admin))
        assertTrue(task.canBeAccessedBy(admin))
    }

    @Test
    fun `addCollaborator should avoid duplicates and set updatedAt`() {
        val owner = user(1L)
        val collaborator = user(2L)
        val task = task(owner, board(owner))

        assertTrue(task.addCollaborator(collaborator))
        val firstUpdate = task.updatedAt
        assertNotNull(firstUpdate)
        assertFalse(task.addCollaborator(collaborator))
        assertEquals(1, task.collaborators.size)
        assertEquals(firstUpdate, task.updatedAt)
    }

    @Test
    fun `changeStatus should enforce transitions`() {
        val owner = user(1L)
        val task = task(owner, board(owner))

        task.changeStatus(TaskStatus.IN_PROGRESS)
        assertEquals(TaskStatus.IN_PROGRESS, task.status)

        assertThrows(InvalidTaskStatusTransitionException::class.java) {
            task.changeStatus(TaskStatus.COMPLETED)
        }
    }
}
