package com.synchtask.mappers

import com.synchtask.dtos.user.UserOptionDTO
import com.synchtask.dtos.user.UserPublicDTO
import com.synchtask.dtos.user.UserResponseDTO
import com.synchtask.entities.User

object UserMapper {

    fun toResponseDTO(user: User): UserResponseDTO {
        return UserResponseDTO(
            id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
            name = user.name,
            email = user.email,
            profilePictureUrl = user.profilePictureUrl ?: "N/A"
        )
    }

    fun toResponseDTOList(users: List<User>): List<UserResponseDTO> {
        return users.map { toResponseDTO(it) }
    }

    fun toOptionDTO(user: User): UserOptionDTO {
        return UserOptionDTO(
            id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
            name = user.name,
            profilePictureUrl = user.profilePictureUrl
        )
    }

    fun toPublicDTO(user: User): UserPublicDTO {
        return UserPublicDTO(
            id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
            name = user.name,
            profilePictureUrl = user.profilePictureUrl
        )
    }
}

