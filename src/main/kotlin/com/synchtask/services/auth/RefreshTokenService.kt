package com.synchtask.services.auth

import com.synchtask.entities.RefreshToken
import com.synchtask.user.domain.entity.User
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    private val logger = LoggerFactory.getLogger(RefreshTokenService::class.java)

    fun createRefreshToken(user: User): RefreshToken {
        val refreshToken = RefreshToken(
            user = user,
            token = generateSecureToken(),
            expiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS)
        )
        return refreshTokenRepository.save(refreshToken)
    }

    fun validateRefreshToken(token: String): RefreshToken {
        val refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow { ResourceNotFoundException("Invalid refresh token") }

        require(!refreshToken.isRevoked) { "Refresh token is revoked" }
        require(refreshToken.expiryDate.isAfter(LocalDateTime.now())) { "Refresh token is expired" }

        return refreshToken
    }

    @Transactional
    fun revokeToken(token: String) {
        val refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow { ResourceNotFoundException("Refresh token not found") }

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
        return java.util.UUID.randomUUID().toString()
    }

    companion object {
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 7L
    }
}
