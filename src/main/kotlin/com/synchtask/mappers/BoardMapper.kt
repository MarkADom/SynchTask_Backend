package com.synchtask.mappers

import com.synchtask.dtos.board.BoardResponseDTO
import com.synchtask.entities.Board

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
