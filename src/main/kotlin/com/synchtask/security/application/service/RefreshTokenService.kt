package com.synchtask.security.application.service

import com.synchtask.security.domain.entity.RefreshToken
import com.synchtask.security.domain.repository.RefreshTokenRepository
import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.user.domain.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val logger = LoggerFactory.getLogger(RefreshTokenService::class.java)

    fun createRefreshToken(user: User): RefreshToken {
        val refreshToken =
            RefreshToken(
                user = user,
                token = generateSecureToken(),
                expiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS)
            )
        return refreshTokenRepository.save(refreshToken)
    }

    fun validateRefreshToken(token: String): RefreshToken {
        val refreshToken =
            refreshTokenRepository.findByToken(token)
                .orElseThrow { InvalidCredentialsException("Invalid refresh token") }
        if (refreshToken.isRevoked) {
            throw InvalidCredentialsException("Refresh token is revoked")
        }
        if (!refreshToken.expiryDate.isAfter(LocalDateTime.now())) {
            throw InvalidCredentialsException("Refresh token is expired")
        }
        return refreshToken
    }

    @Transactional
    fun revokeToken(token: String) {
        val refreshToken =
            refreshTokenRepository.findByToken(token)
                .orElseThrow { InvalidCredentialsException("Refresh token not found") }

        refreshToken.isRevoked = true
        refreshTokenRepository.save(refreshToken)
        logger.info("Refresh token revoked: $token")
    }

    @Transactional
    fun revokeTokensForUser(user: User) {
        val tokens: List<RefreshToken> = refreshTokenRepository.findAllByUserAndIsRevokedFalse(user)
        if (tokens.isNotEmpty()) {
            tokens.forEach { token -> token.isRevoked = true } // ✅ Corrected `forEach` loop
            refreshTokenRepository.saveAll(tokens) // ✅ Save updated tokens
            logger.info("Revoked all refresh tokens for user: ${user.email}")
        }
    }

    private fun generateSecureToken(): String {
        return UUID.randomUUID().toString()
    }

    companion object {
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 7L
    }
}
