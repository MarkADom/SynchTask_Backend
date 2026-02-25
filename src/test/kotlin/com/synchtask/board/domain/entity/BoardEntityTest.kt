package com.synchtask.board.domain.entity

import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardEntityTest {
    private fun user(id: Long, email: String): User =
        User(
            id = id,
            name = email.substringBefore("@"),
            email = email,
            passwordHash = "hash",
            role = UserRole.USER
        )

    @Test
    fun `update should apply non-null fields and set updatedAt`() {
        val owner = user(1L, "owner@test.com")
        val board = Board(
            id = 10L,
            name = "Initial",
            color = "#000000",
            description = "Old",
            owner = owner
        )

        board.update(
            name = "Updated",
            color = "#111111",
            description = "New"
        )

        assertEquals("Updated", board.name)
        assertEquals("#111111", board.color)
        assertEquals("New", board.description)
        assertNotNull(board.updatedAt)
    }

    @Test
    fun `update should keep current values for null fields`() {
        val owner = user(
            1L,
            "owner@test.com"
        )

        val board = Board(
            id = 10L,
            name = "Initial",
            color = "#000000",
            description = "Old",
            owner = owner
        )

        board.update(
            name = null,
            color = null,
            description = null
        )

        assertEquals("Initial", board.name)
        assertEquals("#000000", board.color)
        assertEquals("Old", board.description)
        assertNotNull(board.updatedAt)
    }

    @Test
    fun `hasAccess should allow membership only`() {
        val owner = user(1L, "owner@test.com")
        val member = user(2L, "member@test.com")
        val outsider = user(3L, "out@test.com")
        val board = Board(id = 10L, name = "Board", owner = owner)
        board.members.add(
            BoardMember(
                board = board,
                user = member,
                role = MembershipRole.COLLABORATOR
            )
        )

        assertFalse(board.hasAccess(owner))
        assertTrue(board.hasAccess(member))
        assertFalse(board.hasAccess(outsider))
    }

    @Test
    fun `isOwnedBy should honor membership owner only`() {
        val owner = user(1L, "owner@test.com")
        val membershipOwner = user(2L, "member-owner@test.com")
        val membershipCollaborator = user(3L, "member-collab@test.com")
        val outsider = user(4L, "out@test.com")
        val board = Board(
            id = 10L,
            name = "Board",
            owner = owner
        )

        board.members.add(
            BoardMember(
                board = board,
                user = membershipOwner,
                role = MembershipRole.OWNER
            )
        )
        board.members.add(
            BoardMember(
                board = board,
                user = membershipCollaborator,
                role = MembershipRole.COLLABORATOR
            )
        )

        assertFalse(board.isOwnedBy(owner))
        assertTrue(board.isOwnedBy(membershipOwner))
        assertFalse(board.isOwnedBy(membershipCollaborator))
        assertFalse(board.isOwnedBy(outsider))
    }

    @Test
    fun `admin should always have board access and ownership`() {
        val owner = user(1L, "owner@test.com")
        val admin =
            User(
                id = 99L,
                name = "admin",
                email = "admin@test.com",
                passwordHash = "hash",
                role = UserRole.ADMIN
            )
        val board = Board(
            id = 10L, name = "Board",
            owner = owner
        )

        assertTrue(board.hasAccess(admin))
        assertTrue(board.isOwnedBy(admin))
    }


}
