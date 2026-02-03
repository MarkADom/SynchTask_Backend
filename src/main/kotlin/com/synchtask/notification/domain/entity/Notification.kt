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
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    indexes = [Index(name = "idx_notification_created_at", columnList = "recipient_id, created_at")]
)
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,

    @Column(nullable = false, length = 500)
    val message: String,

    @Column(nullable = false)
    var isRead: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,

    @Column(name = "group_id")
    val groupId: Long? = null,

    @Column(name = "delivered", nullable = false)
    var delivered: Boolean = false
)
