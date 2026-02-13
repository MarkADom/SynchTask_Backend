package com.synchtask.user.presentation.mapper

import com.synchtask.shared.presentation.mapper.MapperSupport.requireId
import com.synchtask.user.application.dto.UserOptionDTO
import com.synchtask.user.application.dto.UserPublicDTO
import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.domain.entity.User

object UserMapper {

    fun toResponseDTO(user: User): UserResponseDTO =
        UserResponseDTO(
            id = requireId(user.id, "User"),
            name = user.name,
            email = user.email,
            profilePictureUrl = user.profilePictureUrl ?: "N/A"
        )

    fun toResponseDTOList(users: List<User>): List<UserResponseDTO> =
        users.map(::toResponseDTO)

    fun toOptionDTO(user: User): UserOptionDTO =
        UserOptionDTO(
            id = requireId(user.id, "User"),
            name = user.name,
            profilePictureUrl = user.profilePictureUrl
        )

    fun toPublicDTO(user: User): UserPublicDTO =
        UserPublicDTO(
            id = requireId(user.id, "User"),
            name = user.name,
            profilePictureUrl = user.profilePictureUrl
        )
}

