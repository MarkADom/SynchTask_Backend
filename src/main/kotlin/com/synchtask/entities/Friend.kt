package com.synchtask.entities

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
    name = "friends",
    indexes = [
        Index(name = "idx_friend_status", columnList = "status"),
        Index(name = "idx_friend_created_at", columnList = "createdAt")
    ]
)
data class Friend(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)  // It avoids loading automatically
    @Fetch(FetchMode.JOIN) // Reduce multiple queries
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "friend_id", nullable = false)
    val friend: User,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FriendshipStatus = FriendshipStatus.PENDING,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class FriendshipStatus {
    PENDING, ACCEPTED, BLOCKED, REJECTED
}
