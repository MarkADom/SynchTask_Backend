package com.synchtask.board.domain.entity

import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BoardMemberTest {
    @Test
    fun `should create board membership with creator`() {
        val owner = testUser(id = 1L)
        val board =
            Board(
                id = 20L,
                name = "Board Alpha",
                owner = owner
            )
        val collaborator = testUser(id = 2L, email = "collab@synchtask.com")

        val boardMember =
            BoardMember(
                id = 200L,
                board = board,
                user = collaborator,
                role = MembershipRole.OWNER,
                createdAt = LocalDateTime.now(),
                createdByUser = owner
            )

        assertEquals(200L, boardMember.id)
        assertEquals(board.id, boardMember.board.id)
        assertEquals(collaborator.id, boardMember.user.id)
        assertEquals(MembershipRole.OWNER, boardMember.role)
        assertNotNull(boardMember.createdByUser)
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
