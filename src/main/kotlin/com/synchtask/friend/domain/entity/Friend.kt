package com.synchtask.friend.domain.entity

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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * Friend relationship entity.
 *
 * Represents a friendship request and its lifecycle between two users.
 */
@Entity
@Table(
    name = "friends",
    indexes = [
        Index(name = "idx_friend_status", columnList = "status"),
        Index(name = "idx_friend_created_at", columnList = "created_at"),
        Index(name = "idx_friend_requester_status", columnList = "requester_id, status"),
        Index(name = "idx_friend_friend_status", columnList = "friend_id, status")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_friend_requester_friend",
            columnNames = ["requester_id", "friend_id"]
        )
    ]
)
data class Friend(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    val friend: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FriendshipStatus = FriendshipStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Friendship lifecycle states.
 */
enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    BLOCKED,
    REJECTED
}
