package com.synchtask.user.domain.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.friend.domain.entity.Friend
import com.synchtask.notification.domain.entity.Notification
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * User aggregate root.
 *
 * Represents a platform user with authentication, profile and relationships.
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email", unique = true),
        Index(name = "idx_user_name", columnList = "name"),
        Index(name = "idx_user_last_activity", columnList = "last_activity"),
        Index(name = "idx_user_created_at", columnList = "created_at")
    ]
)
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(name = "profile_picture_url", length = 2048)
    var profilePictureUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login")
    var lastLogin: LocalDateTime? = null,

    @Column(name = "last_activity")
    var lastActivity: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "is_online", nullable = false)
    var isOnline: Boolean = false,

    @Column(name = "onboarding_notified", nullable = false)
    var onboardingNotified: Boolean = false,

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    val notifications: MutableSet<Notification> = mutableSetOf(),

    @OneToMany(mappedBy = "requester", fetch = FetchType.LAZY)
    val sentFriendRequests: MutableSet<Friend> = mutableSetOf(),

    @OneToMany(mappedBy = "friend", fetch = FetchType.LAZY)
    val receivedFriendRequests: MutableSet<Friend> = mutableSetOf(),

    @ManyToMany(mappedBy = "participants", fetch = FetchType.LAZY)
    val chatRooms: MutableSet<ChatRoom> = mutableSetOf()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}

/**
 * User roles within the platform.
 */
enum class UserRole {
    ADMIN,
    OWNER,
    COLLABORATOR,
    USER
}
