package com.synchtask.managers

import com.synchtask.context.AuthServiceContext
import com.synchtask.dtos.user.UserRegistrationDTO
import com.synchtask.entities.User
import org.springframework.stereotype.Component

@Component
class AuthManager(
    private val authServiceContext: AuthServiceContext,
) {

    fun registerUser(userRegistrationDTO: UserRegistrationDTO): User {
        return authServiceContext.userService.createUser(
            userRegistrationDTO.toUser(authServiceContext.passwordEncoder)
        )
    }

    fun authenticateUser(email: String, password: String): Map<String, String> {
        return authServiceContext.authService.authenticate(email, password)
    }

    fun refreshJwt(refreshToken: String): String {
        val tokenEntity = authServiceContext.refreshTokenService.validateRefreshToken(refreshToken)
        val userDetails = authServiceContext.userDetailsService.loadUserByUsername(tokenEntity.user.email)
        return authServiceContext.jwtTokenProvider.generateToken(userDetails)
    }

    fun logoutUser(email: String) {
        val user = authServiceContext.userService.getUserByEmail(email)
        if (user != null) {
            authServiceContext.refreshTokenService.revokeTokensForUser(user)
        }
    }
}
