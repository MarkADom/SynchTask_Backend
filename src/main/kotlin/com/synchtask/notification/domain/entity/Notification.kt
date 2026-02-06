package com.synchtask.notification.domain.entity

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
 * Notification entity.
 *
 * Represents a notification delivered to a specific user.
 */
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(
            name = "idx_notification_recipient_created",
            columnList = "recipient_id, created_at"
        ),
        Index(
            name = "idx_notification_recipient_read",
            columnList = "recipient_id, is_read"
        )
    ]
)
data class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,

    @Column(nullable = false, length = 500)
    val message: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,

    /**
     * Optional grouping identifier (e.g. multiple notifications for the same event).
     */
    @Column(name = "group_id", nullable = true)
    val groupId: Long? = null,

    @Column(name = "delivered", nullable = false)
    var delivered: Boolean = false
)
