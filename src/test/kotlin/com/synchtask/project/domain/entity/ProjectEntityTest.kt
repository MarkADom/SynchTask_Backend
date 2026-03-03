package com.synchtask.project.domain.entity

import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProjectEntityTest {
    private fun user(id: Long, role: UserRole = UserRole.USER): User =
        User(
            id = id,
            name = "User$id",
            email = "user$id@test.com",
            passwordHash = "hash",
            role = role
        )

    private fun project(owner: User) =
        Project(
            id = 10L,
            name = "Project",
            description = "Desc",
            tag = "tag",
            color = "#000000",
            owner = owner,
            dueDate = LocalDate.now()
        )

    @Test
    fun `hasAccess should allow membership only`() {
        val owner = user(1L)
        val memberByRelation = user(2L)
        val outsider = user(3L)
        val project = project(owner)

        project.projectMembers.add(
            ProjectMember(
                project = project,
                user = memberByRelation,
                role = MembershipRole.COLLABORATOR
            )
        )
        assertFalse(project.hasAccess(owner))
        assertTrue(project.hasAccess(memberByRelation))
        assertFalse(project.hasAccess(outsider))
    }

    @Test
    fun `isOwnedBy should honor membership owner only`() {
        val owner = user(1L)
        val membershipOwner = user(2L)
        val collaborator = user(3L)
        val outsider = user(4L)
        val project = project(owner)

        project.projectMembers.add(
            ProjectMember(
                project = project,
                user = membershipOwner,
                role = MembershipRole.OWNER
            )
        )
        project.projectMembers.add(
            ProjectMember(
                project = project,
                user = collaborator,
                role = MembershipRole.COLLABORATOR
            )
        )

        assertFalse(project.isOwnedBy(owner))
        assertTrue(project.isOwnedBy(membershipOwner))
        assertFalse(project.isOwnedBy(collaborator))
        assertFalse(project.isOwnedBy(outsider))
    }

    @Test
    fun `admin should always have access and ownership`() {
        val owner = user(1L)
        val admin = user(99L, UserRole.ADMIN)
        val project = project(owner)

        assertTrue(project.hasAccess(admin))
        assertTrue(project.isOwnedBy(admin))
    }
}
