package com.synchtask.project.domain.entity

import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ProjectMemberTest {
    @Test
    fun `should create project membership with expected values`() {
        val owner = testUser(id = 1L)
        val memberUser = testUser(id = 2L, email = "member@synchtask.com")
        val project =
            Project(
                id = 10L,
                name = "Project Alpha",
                owner = owner,
                dueDate = LocalDate.now().plusDays(10)
            )

        val projectMember =
            ProjectMember(
                id = 100L,
                project = project,
                user = memberUser,
                role = MembershipRole.COLLABORATOR,
                createdAt = LocalDateTime.now().minusMinutes(5),
                createdByUser = null
            )

        assertEquals(100L, projectMember.id)
        assertEquals(project.id, projectMember.project.id)
        assertEquals(memberUser.id, projectMember.user.id)
        assertEquals(MembershipRole.COLLABORATOR, projectMember.role)
        assertNull(projectMember.createdByUser)
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
