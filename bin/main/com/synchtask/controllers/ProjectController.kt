package com.synchtask.controllers

import com.synchtask.dtos.project.ProjectCreateDTO
import com.synchtask.dtos.project.ProjectResponseDTO
import com.synchtask.dtos.project.ProjectUpdateDTO
import com.synchtask.services.project.ProjectService
import com.synchtask.services.user.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * ProjectController
 *
 * REST endpoints for managing Projects.
 * All operations are secured to the authenticated user.
 */
@RestController
@RequestMapping("/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val userService: UserService
) {

    /**
     * Create a new project for the authenticated user.
     *
     * @param dto Project creation payload.
     * @param user Spring Security authenticated user principal.
     * @return Created ProjectResponseDTO.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun createProject(
        @RequestBody @Valid dto: ProjectCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ProjectResponseDTO {
        val owner = getUserOrThrow(user)
        return projectService.create(dto, owner)
    }

    /**
     * List all projects owned by the authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listProjects(
        @AuthenticationPrincipal user: UserDetails
    ): List<ProjectResponseDTO> {
        val owner = getUserOrThrow(user)
        return projectService.listAll(owner)
    }

    /**
     * Get details of a single project by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getProject(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ProjectResponseDTO {
        val owner = getUserOrThrow(user)
        return projectService.getById(id, owner)
    }

    /**
     * Update a project by ID.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateProject(
        @PathVariable id: Long,
        @RequestBody @Valid dto: ProjectUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ProjectResponseDTO {
        val owner = getUserOrThrow(user)
        return projectService.update(id, dto, owner)
    }

    /**
     * Delete a project by ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    fun deleteProject(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ) {
        val owner = getUserOrThrow(user)
        projectService.delete(id, owner)
    }

    /**
     * Internal helper to resolve UserDetails into domain User entity.
     */
    private fun getUserOrThrow(user: UserDetails) =
        userService.getUserByEmail(user.username)
            ?: throw IllegalArgumentException("User not found: ${user.username}")
}
