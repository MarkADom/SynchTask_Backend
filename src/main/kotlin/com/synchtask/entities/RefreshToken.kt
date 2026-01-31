package com.synchtask.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreRemove
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_expiry", columnList = "expiryDate"),
        Index(name = "idx_refresh_revoked", columnList = "isRevoked")
    ]
)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // It avoids loading automatically
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val expiryDate: LocalDateTime = LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS),

    @Column(nullable = false)
    var isRevoked: Boolean = false,
) {
    companion object {
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 7L
    }

    @PreRemove
    fun preRemove() {
        this.isRevoked = true
    }
}



