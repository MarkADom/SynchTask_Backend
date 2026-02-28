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
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `update with boards emits project boards updated activity`() {
        val project = Project(
            id = 70L,
            name = "Project",
            description = "desc",
            owner = owner,
            dueDate = LocalDate.now()
        )
        val boardOne = com.synchtask.board.domain.entity.Board(
            id = 20L,
            name = "B1",
            owner = owner
        )
        val boardTwo = com.synchtask.board.domain.entity.Board(
            id = 21L,
            name = "B2",
            owner = owner
        )

        every { projectRepository.findById(project.id!!) } returns java.util.Optional.of(project)
        every { projectMemberRepository.findByProjectIdAndUserId(project.id!!, owner.id!!) } returns

            ProjectMember(
                project = project,
                user = owner,
                role = MembershipRole.OWNER,
                createdByUser = owner
            )
        every { boardRepository.findAllById(listOf(boardOne.id!!, boardTwo.id!!)) } returns listOf(boardOne, boardTwo)
        every { projectRepository.save(project) } returns project

        service.update(
            project.id!!,
            com.synchtask.project.application.dto.ProjectUpdateDTO(boardIds = listOf(boardOne.id!!, boardTwo.id!!)),
            owner
        )

        verify(exactly = 1) {
            activityService.record(
                actor = owner,
                type = com.synchtask.activity.domain.model.ActivityType.PROJECT_UPDATED,
                referenceId = project.id,
                description = any(),
                contextSnapshot = null
            )
        }
        verify(exactly = 1) {
            activityService.record(
                actor = owner,
                type = com.synchtask.activity.domain.model.ActivityType.PROJECT_BOARDS_UPDATED,
                referenceId = project.id,
                description = any(),
                contextSnapshot = null
            )
        }
    }

    @Test
    fun `delete records activity snapshot with owner and members`() {
        val project = Project(
            id = 71L,
            name = "Project",
            description = "desc",
            owner = owner,
            dueDate = LocalDate.now()
        )
        val member = User(
            id = 2L,
            name = "Member",
            email = "member@test.com",
            passwordHash = "hash"
        )

        every { projectRepository.findById(project.id!!) } returns java.util.Optional.of(project)
        every { projectMemberRepository.findByProjectIdAndUserId(project.id!!, owner.id!!) } returns
            ProjectMember(
                project = project,
                user = owner,
                role = MembershipRole.OWNER,
                createdByUser = owner
            )
        every { projectMemberRepository.findAllByProjectId(project.id!!) } returns listOf(
            ProjectMember(
                project =
                    project,
                user = owner,
                role = MembershipRole.OWNER,
                createdByUser = owner
            ),
            ProjectMember(
                project = project,
                user = member,
                role = MembershipRole.COLLABORATOR,
                createdByUser = owner
            )
        )

        service.delete(project.id!!, owner)

        verify(exactly = 1) { projectRepository.delete(project) }
        verify(exactly = 1) {
            activityService.record(
                actor = owner,
                type = com.synchtask.activity.domain.model.ActivityType.PROJECT_DELETED,
                referenceId = project.id,
                description = any(),
                contextSnapshot = withArg { snapshot ->
                    kotlin.test.assertEquals(owner.email, snapshot?.ownerEmail)
                    kotlin.test.assertEquals(setOf(owner.email, member.email), snapshot?.memberEmails)
                })
        }
    }

    @Test
    fun `getById throws when actor has no project access`() {
        val project = Project(
            id = 72L,
            name = "Project",
            description = "desc",
            owner = owner,
            dueDate = LocalDate.now()
        )

        every { projectRepository.findById(project.id!!) } returns java.util.Optional.of(project)
        every { projectMemberRepository.existsByProjectIdAndUserId(project.id!!, owner.id!!) } returns false

        assertThrows(NoSuchElementException::class.java) {
            service.getById(project.id!!, owner)
        }
    }


    @Test
    fun `listAll resolves project scope through ProjectMemberRepository`() {
        every { projectMemberRepository.findAllByUserId(owner.id!!) } returns emptyList()

        val result = service.listAll(owner)

        assertEquals(0, result.size)
        verify(exactly = 1) { projectMemberRepository.findAllByUserId(owner.id!!) }
    }
}
