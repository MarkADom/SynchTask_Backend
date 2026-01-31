package com.synchtask.managers

import com.synchtask.context.AuthServiceContext
import com.synchtask.dtos.user.UserRegistrationDTO
import com.synchtask.entities.User
import org.springframework.stereotype.Component

/**
 * **Authentication Manager**
 *
 * Handles authentication workflows by combining multiple authentication services.
 */
@Component
class AuthManager(
    private val authServiceContext: AuthServiceContext,
) {
    /**
     * **Registers a new user**
     */
    fun registerUser(userRegistrationDTO: UserRegistrationDTO): User {
        return authServiceContext.userService.createUser(
            userRegistrationDTO.toUser(authServiceContext.passwordEncoder)
        )
    }

    /**
     * **Authenticates a user and returns access + refresh tokens.**
     */
    fun authenticateUser(email: String, password: String): Map<String, String> {
        return authServiceContext.authService.authenticate(email, password)
    }

    /**
     * **Refresh JWT using a valid Refresh Token**
     */
    fun refreshJwt(refreshToken: String): String {
        val tokenEntity = authServiceContext.refreshTokenService.validateRefreshToken(refreshToken)
        val userDetails = authServiceContext.userDetailsService.loadUserByUsername(tokenEntity.user.email)
        return authServiceContext.jwtTokenProvider.generateToken(userDetails)
    }

    /**
     * **Logs out a user and revokes all active refresh tokens.**
     */
    fun logoutUser(email: String) {
        val user = authServiceContext.userService.getUserByEmail(email)
        if (user != null) {
            authServiceContext.refreshTokenService.revokeTokensForUser(user)
        }
    }
}
