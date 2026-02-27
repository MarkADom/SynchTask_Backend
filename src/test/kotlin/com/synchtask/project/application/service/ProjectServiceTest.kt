package com.synchtask.project.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.project.application.dto.ProjectCreateDTO
import com.synchtask.project.domain.entity.Project
import com.synchtask.project.domain.entity.ProjectMember
import com.synchtask.project.domain.repository.ProjectMemberRepository
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class ProjectServiceTest {
    private lateinit var projectRepository: ProjectRepository
    private lateinit var projectMemberRepository: ProjectMemberRepository
    private lateinit var boardRepository: BoardRepository
    private lateinit var activityService: ActivityService
    private lateinit var service: ProjectService

    private val owner = User(
        id = 1L,
        name = "Owner",
        email = "owner@test.com",
        passwordHash = "hash"
    )

    @BeforeEach
    fun setup() {
        projectRepository = mockk(relaxed = true)
        projectMemberRepository = mockk(relaxed = true)
        boardRepository = mockk(relaxed = true)
        activityService = mockk(relaxed = true)

        service = ProjectService(projectRepository, projectMemberRepository, boardRepository, activityService)
    }

    @Test
    fun `create writes owner membership via ProjectMemberRepository`() {
        val dto = ProjectCreateDTO(
            name = "Project",
            description = "desc",
            dueDate = LocalDate.now(),
            boardIds = emptyList()
        )

        every { boardRepository.findAllById(emptyList<Long>()) } returns emptyList()

        val projectSlot = slot<Project>()
        every { projectRepository.save(capture(projectSlot)) } answers {
            val p = projectSlot.captured
            Project(
                id = 99L,
                name = p.name,
                description = p.description,
                tag = p.tag,
                color = p.color,
                owner = p.owner,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt,
                dueDate = p.dueDate,
                projectMembers = p.projectMembers,
                boards = p.boards
            )
        }

        val memberSlot = slot<ProjectMember>()
        every { projectMemberRepository.save(capture(memberSlot)) } answers { memberSlot.captured }

        service.create(dto, owner)

        verify(exactly = 1) { projectRepository.save(any()) }
        verify(exactly = 1) { projectMemberRepository.save(any()) }

        assertEquals(99L, memberSlot.captured.project.id)
        assertEquals(owner.id, memberSlot.captured.user.id)
        assertEquals(MembershipRole.OWNER, memberSlot.captured.role)
        assertEquals(owner.id, memberSlot.captured.createdByUser?.id)
    }

    @Test
    fun `listAll resolves project scope through ProjectMemberRepository`() {
        every { projectMemberRepository.findAllByUserId(owner.id!!) } returns emptyList()

        val result = service.listAll(owner)

        assertEquals(0, result.size)
        verify(exactly = 1) { projectMemberRepository.findAllByUserId(owner.id!!) }
    }
}
