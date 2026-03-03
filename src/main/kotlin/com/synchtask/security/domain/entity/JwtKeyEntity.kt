package com.synchtask.security.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * JWT signing key entity.
 *
 * Stores private keys used to sign JWT tokens.
 * Keys are immutable and ordered by creation time.
 */
@Entity
@Table(
    name = "jwt_keys",
    indexes = [
        Index(name = "idx_jwt_keys_created_at", columnList = "created_at")
    ]
)
class JwtKeyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    val privateKey: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
