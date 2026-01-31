package com.synchtask.mappers

import com.synchtask.dtos.user.UpdateUserDTO
import com.synchtask.dtos.user.UserOptionDTO
import com.synchtask.dtos.user.UserPublicDTO
import com.synchtask.dtos.user.UserResponseDTO
import com.synchtask.entities.User

/**
 * **UserMapper**
 *
 * Converts User entities into DTOs to prevent exposing sensitive fields.
 */
object UserMapper {

    /**
     * Converts a User entity to a UserResponseDTO.
     */
    fun toResponseDTO(user: User): UserResponseDTO {
        return UserResponseDTO(
            id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
            name = user.name,
            email = user.email,
            profilePictureUrl = user.profilePictureUrl ?: "N/A"
        )
    }

    /**
     * Converts a list of User entities to a list of UserResponseDTOs.
     */
    fun toResponseDTOList(users: List<User>): List<UserResponseDTO> {
        return users.map { toResponseDTO(it) }
    }

    /**
     * Converts a User entity to a UserOptionDTO.
     */
    fun toOptionDTO(user: User): UserOptionDTO {
        return UserOptionDTO(
            id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
            name = user.name,
            profilePictureUrl = user.profilePictureUrl
        )
    }

    /**
     * Converts a User entity to a public-facing DTO.
     */
    fun toPublicDTO(user: User): UserPublicDTO {
        return UserPublicDTO(
            id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
            name = user.name,
            profilePictureUrl = user.profilePictureUrl
        )
    }
}

