package com.synchtask.project.presentation.controller

import com.synchtask.project.application.dto.ProjectCreateDTO
import com.synchtask.project.application.dto.ProjectResponseDTO
import com.synchtask.project.application.dto.ProjectUpdateDTO
import com.synchtask.project.application.service.ProjectService
import com.synchtask.user.application.service.AuthenticatedUserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Project HTTP endpoints.
 *
 * Access control and ownership checks are enforced in the service layer.
 */
@RestController
@RequestMapping("/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val authenticatedUserService: AuthenticatedUserService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun createProject(
        @RequestBody @Valid dto: ProjectCreateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ProjectResponseDTO {
        val owner = authenticatedUserService.requireUser(user)
        return projectService.create(dto, owner)
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listProjects(@AuthenticationPrincipal user: UserDetails): List<ProjectResponseDTO> {
        val owner = authenticatedUserService.requireUser(user)
        return projectService.listAll(owner)
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getProject(@PathVariable id: Long, @AuthenticationPrincipal user: UserDetails): ProjectResponseDTO {
        val owner = authenticatedUserService.requireUser(user)
        return projectService.getById(id, owner)
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun updateProject(
        @PathVariable id: Long,
        @RequestBody @Valid dto: ProjectUpdateDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ProjectResponseDTO {
        val owner = authenticatedUserService.requireUser(user)
        return projectService.update(id, dto, owner)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    fun deleteProject(@PathVariable id: Long, @AuthenticationPrincipal user: UserDetails) {
        val owner = authenticatedUserService.requireUser(user)
        projectService.delete(id, owner)
    }
}
