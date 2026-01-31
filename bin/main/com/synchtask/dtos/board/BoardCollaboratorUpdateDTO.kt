package com.synchtask.dtos.board

/**
 * DTO used to update the list of collaborators in a board.
 *
 * @property userIds List of user IDs to be added as collaborators.
 */
data class BoardCollaboratorUpdateDTO(
    val userIds: List<Long>
)
