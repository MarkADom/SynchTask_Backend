package com.synchtask.project.application.service

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.project.application.dto.ProjectCreateDTO
import com.synchtask.project.application.dto.ProjectResponseDTO
import com.synchtask.project.application.dto.ProjectUpdateDTO
import com.synchtask.project.domain.entity.Project
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.project.presentation.mapper.ProjectMapper
import com.synchtask.user.domain.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val boardRepository: BoardRepository,
    private val activityService: ActivityService,
) {
    @Transactional
    fun create(dto: ProjectCreateDTO, owner: User): ProjectResponseDTO {
        val boards = boardRepository.findAllWithCollaboratorsById(dto.boardIds)
        validateBoardsExist(dto.boardIds, boards)

        val project =
            Project(
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

        activityService.record(
            actor = owner,
            type = ActivityType.PROJECT_CREATED,
            referenceId = savedProject.id,
            description = "Project '${savedProject.name}' criado"
        )

        return ProjectMapper.toResponse(savedProject)
    }

    @Transactional(readOnly = true)
    fun listAll(owner: User): List<ProjectResponseDTO> = projectRepository.findAllByOwner(owner)
        .map(ProjectMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: Long, owner: User): ProjectResponseDTO = projectRepository.findById(id)
        .filter { it.owner.id == owner.id }
        .orElseThrow { NoSuchElementException("Project $id not found or unauthorized") }
        .let(ProjectMapper::toResponse)

    @Transactional
    fun update(id: Long, dto: ProjectUpdateDTO, owner: User): ProjectResponseDTO {
        val project =
            projectRepository.findById(id)
                .filter { it.owner.id == owner.id }
                .orElseThrow { NoSuchElementException("Project $id not found or unauthorized") }

        var boardsUpdated = false

        dto.name?.let { project.name = it }
        dto.description?.let { project.description = it }
        dto.tag?.let { project.tag = it }
        dto.color?.let { project.color = it }
        dto.dueDate?.let { project.dueDate = it }

        dto.boardIds?.let { ids ->
            val boardsFromDB = boardRepository.findAllWithCollaboratorsById(ids)
            validateBoardsExist(ids, boardsFromDB)

            project.boards.forEach { it.project = null }
            project.boards.clear()

            boardsFromDB.forEach { board ->
                board.project = project
                project.boards.add(board)
            }

            boardsUpdated = true
        }

        project.updatedAt = LocalDateTime.now()
        val updated = projectRepository.save(project)

        activityService.record(
            actor = owner,
            type = ActivityType.PROJECT_UPDATED,
            referenceId = updated.id,
            description = "Project '${updated.name}' atualizado"
        )

        if (boardsUpdated) {
            activityService.record(
                actor = owner,
                type = ActivityType.PROJECT_BOARDS_UPDATED,
                referenceId = updated.id,
                description = "Boards do project '${updated.name}' atualizados"
            )
        }

        return updated.let(ProjectMapper::toResponse)
    }

    @Transactional
    fun delete(id: Long, owner: User) {
        val project =
            projectRepository.findById(id)
                .filter { it.owner.id == owner.id }
                .orElseThrow { NoSuchElementException("Project $id not found or unauthorized") }

        val snapshot =
            ActivityContextSnapshot(
                ownerEmail = project.owner.email,
                memberEmails = project.members.map { it.email }.toSet()
            )

        projectRepository.delete(project)

        activityService.record(
            actor = owner,
            type = ActivityType.PROJECT_DELETED,
            referenceId = id,
            description = "Project '${project.name}' removido",
            contextSnapshot = snapshot
        )
    }

    private fun validateBoardsExist(expectedIds: List<Long>, actualBoards: List<Board>) {
        if (actualBoards.size != expectedIds.size) {
            throw IllegalArgumentException("One or more boards not found for provided IDs")
        }
    }
}
