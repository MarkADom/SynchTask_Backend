package com.synchtask.project.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.project.application.dto.ProjectCreateDTO
import com.synchtask.project.application.dto.ProjectUpdateDTO
import com.synchtask.project.domain.entity.Project
import com.synchtask.project.domain.entity.ProjectMember
import com.synchtask.project.domain.repository.ProjectMemberRepository
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.*

class ProjectServiceTest {
    private lateinit var projectRepository: ProjectRepository
    private lateinit var projectMemberRepository: ProjectMemberRepository
    private lateinit var boardRepository: BoardRepository
    private lateinit var activityService: ActivityService
    private lateinit var service: ProjectService

    private val owner = newUser(1L)
    private val other = newUser(2L)

    @BeforeEach
    fun setup() {
        projectRepository = mockk()
        boardRepository = mockk()
        projectMemberRepository = mockk(relaxed = true)
        activityService = mockk(relaxed = true)

        every { projectMemberRepository.existsByProjectIdAndUserId(any(), any()) } returns false
        every { projectMemberRepository.findByProjectIdAndUserId(any(), any()) } returns null

        service = ProjectService(
            projectRepository,
            projectMemberRepository,
            boardRepository,
            activityService
        )
    }

    private fun newUser(id: Long) = User(
        id = id,
        name = "User$id",
        email = "user$id@test.com",
        passwordHash = "hash"
    )

    private fun newBoard(id: Long) = Board(
        id = id,
        name = "Board$id",
        color = "#fff",
        description = "Desc",
        owner = owner,
        collaborators = mutableSetOf(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private fun newProject(id: Long = 10L, owner: User = this.owner) = Project(
        id = id,
        name = "Project",
        description = "Desc",
        tag = "tag",
        color = "#000",
        owner = owner,
        dueDate = LocalDate.now(),
        boards = mutableSetOf(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Test
    fun `should create project and assign boards`() {
        val boards = listOf(newBoard(1), newBoard(2))

        val dto =
            ProjectCreateDTO(
                name = "My Project",
                description = "Desc",
                tag = "tag",
                color = "#111",
                dueDate = LocalDate.now(),
                boardIds = listOf(1, 2)
            )

        every {
            boardRepository.findAllById(dto.boardIds)
        } returns boards

        every {
            projectRepository.save(any())
        } answers {
            val p = firstArg<Project>()
            Project(
                id = 10L,
                name = p.name,
                description = p.description,
                tag = p.tag,
                color = p.color,
                owner = p.owner,
                dueDate = p.dueDate,
                boards = p.boards,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

        every {
            boardRepository.save(any())
        } answers {
            firstArg()
        }

        val result = service.create(dto, owner)

        assertEquals("My Project", result.name)
        verify(exactly = 1) { projectRepository.save(any()) }
        verify(exactly = 2) { boardRepository.save(any()) }
    }

    @Test
    fun `should throw when some boards do not exist on create`() {
        val dto =
            ProjectCreateDTO(
                name = "X",
                description = "blabla",
                tag = null,
                color = null,
                dueDate = null,
                boardIds = listOf(1, 2)
            )

        every { boardRepository.findAllById(dto.boardIds) } returns listOf(newBoard(1))

        assertFailsWith<IllegalArgumentException> {
            service.create(dto, owner)
        }

        verify { projectRepository wasNot Called }
    }

    @Test
    fun `should list projects by membership`() {
        val project = newProject()
        every { projectMemberRepository.findAllByUserId(owner.id!!) } returns listOf(
            ProjectMember(
                project = project,
                user = owner,
                role = MembershipRole.COLLABORATOR
            )
        )
        every { projectRepository.findAllById(listOf(project.id!!)) } returns listOf(project)

        val result = service.listAll(owner)

        assertEquals(1, result.size)
    }

    @Test
    fun `should list all projects for admin`() {
        val admin = User(
            id = 99L,
            name = "Admin",
            email = "admin@test.com",
            passwordHash = "hash",
            role = UserRole.ADMIN
        )
        every { projectRepository.findAll() } returns listOf(newProject(10L), newProject(11L))

        val result = service.listAll(admin)

        assertEquals(2, result.size)
    }

    @Test
    fun `should return empty project list when actor id is null`() {
        val actorWithoutId = User(
            id = null,
            name = "NoId",
            email = "noid@test.com",
            passwordHash = "hash"
        )

        val result = service.listAll(actorWithoutId)

        assertTrue(result.isEmpty())
        verify(exactly = 0) { projectMemberRepository.findAllByUserId(any()) }
    }


    @Test
    fun `should return project when owner`() {
        val project = newProject()

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)
        every { projectMemberRepository.existsByProjectIdAndUserId(project.id!!, owner.id!!) } returns true


        val result = service.getById(project.id!!, owner)

        assertEquals(project.id, result.id)
    }

    @Test
    fun `should throw when project not owned`() {
        val project = newProject(owner = other)

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)

        assertFailsWith<NoSuchElementException> {
            service.getById(project.id!!, owner)
        }
    }

    @Test
    fun `should update project fields`() {
        val project = newProject()

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)
        every { projectMemberRepository.findByProjectIdAndUserId(project.id!!, owner.id!!) } returns
            ProjectMember(project = project, user = owner, role = MembershipRole.OWNER)
        every { projectRepository.save(any()) } answers { firstArg() }

        val dto =
            ProjectUpdateDTO(
                name = "Updated",
                description = "New desc",
                tag = null,
                color = null,
                dueDate = null,
                boardIds = null
            )

        val result = service.update(project.id!!, dto, owner)

        assertEquals("Updated", result.name)
        verify { projectRepository.save(project) }
    }

    @Test
    fun `should update project boards and synchronize owning side`() {
        val project = newProject()
        val existingBoard = newBoard(99).apply { this.project = project }
        project.boards.add(existingBoard)
        val boards = listOf(newBoard(1), newBoard(2))

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)
        every { projectMemberRepository.findByProjectIdAndUserId(project.id!!, owner.id!!) } returns
            ProjectMember(project = project, user = owner, role = MembershipRole.OWNER)
        every { boardRepository.findAllById(listOf(1L, 2L)) } returns boards
        every { projectRepository.save(any()) } answers { firstArg() }

        val dto =
            ProjectUpdateDTO(
                name = null,
                description = null,
                tag = null,
                color = null,
                dueDate = null,
                boardIds = listOf(1L, 2L)
            )

        service.update(project.id!!, dto, owner)

        assertEquals(2, project.boards.size)
        assertNull(existingBoard.project)
        assertTrue(project.boards.all { it.project == project })
    }

    @Test
    fun `should delete project when owner`() {
        val project = newProject()

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)
        every { projectMemberRepository.findByProjectIdAndUserId(project.id!!, owner.id!!) } returns
            ProjectMember(
                project = project,
                user = owner,
                role = MembershipRole.OWNER
            )
        every { projectRepository.delete(project) } just Runs

        service.delete(project.id!!, owner)

        verify { projectRepository.delete(project) }
    }

    @Test
    fun `should throw when deleting project not owned`() {
        val project = newProject(owner = other)

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)

        assertFailsWith<NoSuchElementException> {
            service.delete(project.id!!, owner)
        }
    }

    @Test
    fun `should return project when user has membership access`() {
        val project = newProject(owner = other)

        every { projectRepository.findById(project.id!!) } returns Optional.of(project)
        every { projectMemberRepository.existsByProjectIdAndUserId(project.id!!, owner.id!!) } returns true

        val result = service.getById(project.id!!, owner)

        assertEquals(project.id, result.id)
    }

    @Test
    fun `should update project when user is membership owner`() {
        val project = newProject(owner = other)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project)
        every {
            projectMemberRepository.findByProjectIdAndUserId(project.id!!, owner.id!!)
        } returns ProjectMember(project = project, user = owner, role = MembershipRole.OWNER)
        every { projectRepository.save(any()) } answers { firstArg() }

        val result = service.update(project.id!!, ProjectUpdateDTO(name = "Membership Updated"), owner)

        assertEquals("Membership Updated", result.name)
    }

    @Test
    fun `should return project for admin`() {
        val project = newProject(owner = other)
        val admin =
            User(id = 99L, name = "Admin", email = "admin@test.com", passwordHash = "hash", role = UserRole.ADMIN)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project)

        val result = service.getById(project.id!!, admin)

        assertEquals(project.id, result.id)
    }
}
