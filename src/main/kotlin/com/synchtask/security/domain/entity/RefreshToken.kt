package com.synchtask.security.domain.entity

import com.synchtask.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Refresh token entity.
 *
 * Represents a long-lived token used to renew access tokens.
 * Tokens are revoked explicitly and cleaned up asynchronously.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_expiry", columnList = "expiry_date"),
        Index(name = "idx_refresh_revoked", columnList = "is_revoked"),
        Index(name = "idx_refresh_user_revoked", columnList = "user_id, is_revoked")
    ]
)
data class RefreshToken(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true, length = 512)
    val token: String,

    @Column(name = "expiry_date", nullable = false)
    val expiryDate: LocalDateTime,

    @Column(name = "is_revoked", nullable = false)
    var isRevoked: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val REFRESH_TOKEN_EXPIRY_DAYS = 7L
    }

    fun revoke() {
        this.isRevoked = true
    }

    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean =
        expiryDate.isBefore(now)
}
