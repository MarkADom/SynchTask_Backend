package com.synchtask.project.presentation.mapper

import com.synchtask.board.application.dto.BoardSimpleDTO
import com.synchtask.project.application.dto.ProjectResponseDTO
import com.synchtask.project.domain.entity.Project
import com.synchtask.shared.presentation.mapper.MapperSupport.requireId

object ProjectMapper {

    fun toResponse(project: Project): ProjectResponseDTO =
        ProjectResponseDTO(
            id = requireId(project.id, "Project"),
            name = project.name,
            description = project.description,
            tag = project.tag,
            color = project.color,
            dueDate = project.dueDate,
            members = project.members.map { it.email },
            boards = project.boards.map { board ->
                BoardSimpleDTO(
                    id = requireId(board.id, "Board"),
                    name = board.name
                )
            }
        )
}
