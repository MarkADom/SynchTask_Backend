package com.synchtask.security.infrastructure.jwt

import com.synchtask.user.domain.repository.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

/**
 * Issues and validates JWT tokens using RSA keys.
 */
@Component
class JwtTokenProvider(
    private val jwtKeyManager: JwtKeyManager,
    private val userDetailsService: UserDetailsService,
    private val userRepository: UserRepository,
    @Value("\${jwt.expiration}") private val expiration: Long,
    @Value("\${jwt.issuer}") private val issuer: String,
    @Value("\${jwt.audience}") private val audience: String
) {

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    private val privateKey: PrivateKey by lazy { jwtKeyManager.getPrivateKey() }
    private val publicKey: PublicKey by lazy { jwtKeyManager.getPublicKey() }

    fun generateToken(userDetails: UserDetails): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        val user = userRepository.findByEmail(userDetails.username)
            .orElseThrow { IllegalArgumentException("User not found") }

        return Jwts.builder()
            .subject(user.email)
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("roles", userDetails.authorities.map { it.authority })
            .claim("aud", audience) // Audience is added as a normal claim
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()
    }

    fun validateAndExtractUser(token: String): UserDetails? {
        return try {
            val claims = parseToken(token).payload
            val username = claims.subject ?: return null

            val userDetails = runCatching { userDetailsService.loadUserByUsername(username) }
                .getOrElse {
                    logger.warn("Failed to load user from JWT: ${it.message}")
                    return null
                }

            if (userDetails.authorities.isEmpty()) {
                logger.warn("User has no assigned roles: $username")
                return null
            }

            userDetails
        } catch (ex: JwtException) {
            logger.warn("Invalid JWT token: ${ex.message}")
            null
        }
    }

    fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val headerToken = request.getHeader("Authorization")
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX_LENGTH)

        val queryToken = request.getParameter("token")?.takeIf { it.isNotBlank() }

        return headerToken ?: queryToken
    }

    private fun parseToken(token: String): Jws<Claims> {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val BEARER_PREFIX_LENGTH = BEARER_PREFIX.length
    }
}
