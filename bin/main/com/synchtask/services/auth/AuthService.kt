package com.synchtask.services.auth

import com.synchtask.entities.UserRole
import com.synchtask.exception.InvalidCredentialsException
import com.synchtask.managers.NotificationManager
import com.synchtask.handlers.WelcomeNotificationHandler
import com.synchtask.repositories.UserRepository
import com.synchtask.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * **Authentication Service**
 *
 * Handles login, token generation, and account role management.
 */
@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val notificationManager: NotificationManager,
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Authenticates the user, generates tokens, and sends onboarding notifications if applicable.
     */
    fun authenticate(email: String, rawPassword: String): Map<String, String> {
        logger.info("Attempting authentication for: $email")

        val userDetails: UserDetails = userDetailsService.loadUserByUsername(email)
            ?: throw InvalidCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(rawPassword, userDetails.password)) {
            logger.warn("Invalid credentials for user: $email")
            throw InvalidCredentialsException("Invalid email or password")
        }

        val authentication = UsernamePasswordAuthenticationToken(userDetails, rawPassword, userDetails.authorities)
        authenticationManager.authenticate(authentication)

        val user = userRepository.findByEmail(email)
            .orElseThrow { InvalidCredentialsException("User not found") }

        val accessToken = jwtTokenProvider.generateToken(userDetails)
        val refreshToken = refreshTokenService.createRefreshToken(user)

        /**
         * Send onboarding notifications only if this is the user's first login
         */
        if (user.lastLogin == null && !user.onboardingNotified) {
            notificationManager.handle(user, WelcomeNotificationHandler::class)
            user.onboardingNotified = true
            userRepository.save(user)
        }

        logger.info("JWT and refresh token issued for $email")

        return mapOf(
            "accessToken" to accessToken,
            "refreshToken" to refreshToken.token
        )
    }

    /**
     * Allows ADMIN to change another user's role, enforcing security restrictions.
     */
    fun updateUserRole(adminEmail: String, targetUserId: Long, newRole: UserRole) {
        val adminUser = userRepository.findByEmail(adminEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found") }

        if (adminUser.role != UserRole.ADMIN) {
            logger.warn("Unauthorized role update attempt by $adminEmail")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can update roles")
        }

        val targetUser = userRepository.findById(targetUserId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found") }

        if (newRole == UserRole.ADMIN) {
            logger.warn("Blocked ADMIN role assignment via API")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign ADMIN role via API")
        }

        targetUser.role = newRole
        userRepository.save(targetUser)

        logger.info("Role of ${targetUser.email} changed to $newRole by ADMIN ${adminUser.email}")
    }
}
