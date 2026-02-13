package com.synchtask.user.presentation.mapper

import com.synchtask.user.application.dto.UpdateUserDTO
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import org.springframework.security.crypto.password.PasswordEncoder

object UserCommandMapper {

    fun toNewUser(dto: UserRegistrationDTO, passwordEncoder: PasswordEncoder): User =
        User(
            name = dto.name,
            email = dto.email,
            passwordHash = passwordEncoder.encode(dto.password),
            profilePictureUrl = dto.profilePictureUrl ?: "",
            role = UserRole.USER
        )

    fun toUpdatedUser(dto: UpdateUserDTO, existingUser: User): User =
        User(
            id = existingUser.id,
            name = dto.name,
            email = dto.email,
            passwordHash = dto.passwordHash ?: existingUser.passwordHash,
            profilePictureUrl = dto.profilePictureUrl ?: existingUser.profilePictureUrl.orEmpty(),
            role = existingUser.role,
            createdAt = existingUser.createdAt,
            lastLogin = existingUser.lastLogin,
            lastActivity = existingUser.lastActivity,
            isActive = existingUser.isActive,
            isOnline = existingUser.isOnline,
            onboardingNotified = existingUser.onboardingNotified
        )
}
