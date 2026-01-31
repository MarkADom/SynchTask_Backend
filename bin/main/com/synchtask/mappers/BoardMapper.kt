package com.synchtask.mappers

import com.synchtask.dtos.board.BoardResponseDTO
import com.synchtask.entities.Board

/**
 * BoardMapper
 *
 * Converts Board entities into BoardResponseDTO.
 */
object BoardMapper {

    /**
     * Converts a Board entity to a BoardResponseDTO.
     */
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
