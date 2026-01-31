package com.synchtask.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

/**
 * **User Encryption Keys**
 *
 * Stores the public encryption keys for each user.
 */
@Entity
@Table(name = "user_encryption_keys")
class UserEncryptionKeys(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(nullable = false, length = 512)
    var publicKey: String
)
