package com.synchtask.project.presentation.controller

import com.synchtask.project.application.dto.ProjectCreateDTO
import com.synchtask.project.application.dto.ProjectResponseDTO
import com.synchtask.project.application.dto.ProjectUpdateDTO
import com.synchtask.project.application.service.ProjectService
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.application.service.AuthenticatedUserService
import com.synchtask.user.domain.entity.User
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate
import kotlin.test.assertEquals
import org.springframework.security.core.userdetails.User as SpringUser

class ProjectControllerTest {
    private lateinit var projectService: ProjectService
    private lateinit var authenticatedUserService: AuthenticatedUserService
    private lateinit var controller: ProjectController

    private lateinit var userEntity: User
    private lateinit var userDetails: UserDetails

    @BeforeEach
    fun setup() {
        projectService = mockk()
        authenticatedUserService = mockk()
        controller = ProjectController(projectService, authenticatedUserService)

        userEntity =
            User(
                id = 1L,
                name = "User",
                email = "user@test.com",
                passwordHash = "hash"
            )

        userDetails =
            SpringUser(
                userEntity.email,
                "hash",
                emptyList()
            )
    }

    private fun newProjectResponse(id: Long = 1L, name: String = "Project $id") = ProjectResponseDTO(
        id = id,
        name = name,
        description = "Desc",
        tag = "tag",
        color = "#111",
        dueDate = LocalDate.now(),
        members = emptyList(),
        boards = emptyList()
    )

    @Test
    fun `should create project`() {
        val dto =
            ProjectCreateDTO(
                name = "My Project",
                description = "Desc",
                tag = "tag",
                color = "#111",
                dueDate = LocalDate.now(),
                boardIds = emptyList()
            )

        val response = newProjectResponse()

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { projectService.create(dto, userEntity) } returns response

        val result = controller.createProject(dto, userDetails)

        assertEquals(response.id, result.id)
        assertEquals(response.name, result.name)

        verify(exactly = 1) {
            projectService.create(dto, userEntity)
        }
    }

    @Test
    fun `should list projects`() {
        val projects =
            listOf(
                newProjectResponse(1),
                newProjectResponse(2)
            )

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { projectService.listAll(userEntity) } returns projects

        val result = controller.listProjects(userDetails)

        assertEquals(2, result.size)
        assertEquals("Project 1", result.first().name)

        verify(exactly = 1) {
            projectService.listAll(userEntity)
        }
    }

    @Test
    fun `should get project by id`() {
        val response = newProjectResponse(10)

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { projectService.getById(10L, userEntity) } returns response

        val result = controller.getProject(10L, userDetails)

        assertEquals(10L, result.id)
        assertEquals(response.name, result.name)

        verify(exactly = 1) {
            projectService.getById(10L, userEntity)
        }
    }

    @Test
    fun `should update project`() {
        val dto =
            ProjectUpdateDTO(
                name = "Updated",
                description = "Updated desc",
                tag = "new",
                color = "#000",
                dueDate = LocalDate.now(),
                boardIds = emptyList()
            )

        val response = newProjectResponse(10, "Updated")

        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { projectService.update(10L, dto, userEntity) } returns response

        val result = controller.updateProject(10L, dto, userDetails)

        assertEquals("Updated", result.name)

        verify(exactly = 1) {
            projectService.update(10L, dto, userEntity)
        }
    }

    @Test
    fun `should delete project`() {
        every { authenticatedUserService.requireUser(userDetails) } returns userEntity
        every { projectService.delete(10L, userEntity) } just Runs

        controller.deleteProject(10L, userDetails)

        verify(exactly = 1) {
            projectService.delete(10L, userEntity)
        }
    }

    @Test
    fun `should throw when user not found`() {
        every { authenticatedUserService.requireUser(userDetails) } throws
            ResourceNotFoundException("Authenticated user not found: ${userDetails.username}")

        assertThrows<ResourceNotFoundException> {
            controller.listProjects(userDetails)
        }
    }
}
