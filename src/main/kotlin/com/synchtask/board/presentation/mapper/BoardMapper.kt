package com.synchtask.board.presentation.mapper

import com.synchtask.board.application.dto.BoardResponseDTO
import com.synchtask.board.domain.entity.Board

object BoardMapper {

    fun toResponse(entity: Board): BoardResponseDTO =
        BoardResponseDTO(
            id = entity.id!!,
            name = entity.name,
            color = entity.color,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            ownerName = entity.owner.name
        )
}
