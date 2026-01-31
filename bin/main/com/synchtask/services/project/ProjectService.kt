package com.synchtask.services.project

import com.synchtask.dtos.project.ProjectCreateDTO
import com.synchtask.dtos.project.ProjectResponseDTO
import com.synchtask.dtos.project.ProjectUpdateDTO
import com.synchtask.dtos.project.toResponseDTO
import com.synchtask.entities.Project
import com.synchtask.entities.User
import com.synchtask.repositories.BoardRepository
import com.synchtask.repositories.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * ProjectService
 *
 * Business layer for managing Projects, fully transactional, ownership-safe,
 * Hibernate-consistent, and future-proof for scale.
 */
@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val boardRepository: BoardRepository
) {

    /**
     * Creates a new Project for the given owner.
     * Boards are validated and preloaded before assignment.
     *
     * @param dto Project creation data.
     * @param owner Authenticated user entity.
     * @return Created ProjectResponseDTO.
     */
    @Transactional
    fun create(dto: ProjectCreateDTO, owner: User): ProjectResponseDTO {
        val boards = boardRepository.findAllWithCollaboratorsById(dto.boardIds)
        validateBoardsExist(dto.boardIds, boards)

        val project = Project(
            name = dto.name,
            description = dto.description,
            tag = dto.tag ?: "",
            color = dto.color ?: "#60A5FA",
            owner = owner,
            dueDate = dto.dueDate ?: LocalDate.now()
        )

        val savedProject = projectRepository.save(project)

        boards.forEach { board ->
            board.project = savedProject
            boardRepository.save(board)
        }

        return savedProject.toResponseDTO()
    }

    /**
     * Lists all projects owned by the provided user.
     */
    @Transactional(readOnly = true)
    fun listAll(owner: User): List<ProjectResponseDTO> {
        return projectRepository.findAllByOwner(owner)
            .map(Project::toResponseDTO)
    }

    /**
     * Retrieves a specific project, verifying ownership.
     */
    @Transactional(readOnly = true)
    fun getById(id: Long, owner: User): ProjectResponseDTO {
        val project = projectRepository.findById(id)
            .filter { it.owner.id == owner.id }
            .orElseThrow { NoSuchElementException("Project $id not found or unauthorized") }

        return project.toResponseDTO()
    }

    /**
     * Updates an existing project for the authenticated user with partial fields.
     *
     * @param id Project ID.
     * @param dto ProjectUpdateDTO containing optional update fields.
     * @param owner Authenticated user entity.
     * @return Updated ProjectResponseDTO.
     * @throws NoSuchElementException if not found or unauthorized.
     */
    @Transactional
    fun update(id: Long, dto: ProjectUpdateDTO, owner: User): ProjectResponseDTO {
        val project = projectRepository.findById(id)
            .filter { it.owner.id == owner.id }
            .orElseThrow { NoSuchElementException("Project $id not found or unauthorized") }

        dto.name?.let { project.name = it }
        dto.description?.let { project.description = it }
        dto.tag?.let { project.tag = it }
        dto.color?.let { project.color = it }
        dto.dueDate?.let { project.dueDate = it }
        dto.boardIds?.let { ids ->
            val boardsFromDB = boardRepository.findAllWithCollaboratorsById(ids)
            validateBoardsExist(ids, boardsFromDB)
            project.boards.clear()
            project.boards.addAll(boardsFromDB)
        }

        project.updatedAt = java.time.LocalDateTime.now()

        return projectRepository.save(project).toResponseDTO()
    }

    /**
     * Deletes a project owned by the authenticated user.
     */
    @Transactional
    fun delete(id: Long, owner: User) {
        val project = projectRepository.findById(id)
            .filter { it.owner.id == owner.id }
            .orElseThrow { NoSuchElementException("Project $id not found or unauthorized") }

        projectRepository.delete(project)
    }

    /**
     * Internal validation helper to ensure all board IDs exist.
     */
    private fun validateBoardsExist(expectedIds: List<Long>, actualBoards: List<com.synchtask.entities.Board>) {
        if (actualBoards.size != expectedIds.size) {
            throw IllegalArgumentException("One or more boards not found for provided IDs")
        }
    }
}
