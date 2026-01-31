package com.synchtask.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * **JWT Key Entity**
 * - Stores the private key for JWT signing.
 */
@Entity
@Table(name = "jwt_keys")
data class JwtKeyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    val privateKey: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
