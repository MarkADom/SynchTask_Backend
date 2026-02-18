package com.synchtask.activity.domain.entity

import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
 * Activity aggregate root.
 *
 * Represents a meaningful event that happened in the system.
 */
@Entity
@Table(
    name = "activities",
    indexes = [
        Index(name = "idx_activity_actor", columnList = "actor_id"),
        Index(name = "idx_activity_type", columnList = "type"),
        Index(name = "idx_activity_created_at", columnList = "created_at")
    ]
)
class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    val actor: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: ActivityType,
    @Column(name = "reference_id")
    val referenceId: Long? = null,
    @Column(length = 1000)
    val description: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
