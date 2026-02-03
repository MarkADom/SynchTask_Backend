package com.synchtask.security.application.context

import com.synchtask.security.application.service.AuthService
import com.synchtask.security.application.service.RefreshTokenService
import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import com.synchtask.user.application.service.UserService
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Small container for auth-related dependencies.
 *
 * Keeps the authentication flow wiring explicit
 * without bloating service constructors.
 */
@Component
data class AuthServiceContext(
    val authService: AuthService,
    val refreshTokenService: RefreshTokenService,
    val userDetailsService: UserDetailsService,
    val jwtTokenProvider: JwtTokenProvider,
    val userService: UserService,
    val passwordEncoder: PasswordEncoder,
)
