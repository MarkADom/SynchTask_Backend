package com.synchtask.user.application.service

import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class AuthenticatedUserService(
    private val userService: UserService
) {
    fun requireUser(principal: UserDetails): User = userService.getUserByEmail(principal.username)
        ?: throw ResourceNotFoundException("Authenticated user not found: ${principal.username}")
}
