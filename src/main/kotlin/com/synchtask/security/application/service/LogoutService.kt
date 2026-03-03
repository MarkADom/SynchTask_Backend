package com.synchtask.security.application.service

import com.synchtask.security.domain.repository.RefreshTokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Service

@Service
class LogoutService(
    private val refreshTokenRepository: RefreshTokenRepository
) : LogoutHandler {
    private val logger = LoggerFactory.getLogger(LogoutService::class.java)

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")

        if (token.isNullOrBlank()) {
            logger.warn("Logout failed: No token provided")
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.writer.write("{\"message\": \"No token provided\"}")
            return
        }

        val refreshToken = refreshTokenRepository.findByToken(token)
        if (refreshToken.isPresent) {
            val existingToken = refreshToken.get()
            existingToken.isRevoked = true
            refreshTokenRepository.save(existingToken)
            logger.info("Revoked refresh token for user: ${existingToken.user.email}")
        } else {
            logger.warn("No refresh token found for the provided token")
        }

        SecurityContextHolder.clearContext()
        response.status = HttpServletResponse.SC_OK
        response.writer.write("{\"message\": \"Logout successful\"}")
    }
}
