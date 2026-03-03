package com.synchtask.task.domain.entity

import com.synchtask.board.domain.entity.Board
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TaskMemberTest {
    @Test
    fun `should create task membership with expected aggregate references`() {
        val owner = testUser(1L)
        val board = Board(id = 30L, name = "Board", owner = owner)
        val task =
            Task(
                id = 40L,
                title = "Task",
                description = "desc",
                owner = owner,
                board = board
            )
        val collaborator = testUser(2L, "collab@synchtask.com")

        val taskMember =
            TaskMember(
                id = 300L,
                task = task,
                user = collaborator,
                role = MembershipRole.COLLABORATOR,
                createdAt = LocalDateTime.now(),
                createdByUser = owner
            )

        assertEquals(300L, taskMember.id)
        assertEquals(task.id, taskMember.task.id)
        assertEquals(collaborator.id, taskMember.user.id)
        assertEquals(MembershipRole.COLLABORATOR, taskMember.role)
        assertEquals(owner.id, taskMember.createdByUser?.id)
    }

    private fun testUser(id: Long, email: String = "owner@synchtask.com") =
        User(
            id = id,
            name = "User $id",
            email = email,
            passwordHash = "hash",
            role = UserRole.USER
        )
}
