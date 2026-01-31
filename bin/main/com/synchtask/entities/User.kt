package com.synchtask.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime

/**
 * **User Entity**
 *
 * Represents an application user with identity, profile and role-based access control.
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email", unique = true),
        Index(name = "idx_user_name", columnList = "name"),
        Index(name = "idx_user_last_activity", columnList = "last_activity"),
        Index(name = "idx_user_created_date", columnList = "created_date")
    ]
)
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "profile_picture_url")
    var profilePictureUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Column(nullable = false, updatable = false)
    val createdDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login")
    var lastLogin: LocalDateTime? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false)
    var isOnline: Boolean = false,

    @Column(name = "last_activity")
    var lastActivity: LocalDateTime? = null,

    @Column(name = "onboarding_notified", nullable = false)
    var onboardingNotified: Boolean = false,

    /** --- Relationships --- */

    @OneToMany(mappedBy = "recipient", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    val notifications: MutableSet<Notification> = mutableSetOf(),

    @OneToMany(mappedBy = "requester", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    val sentFriendRequests: MutableSet<Friend> = mutableSetOf(),

    @OneToMany(mappedBy = "friend", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    val receivedFriendRequests: MutableSet<Friend> = mutableSetOf(),

    @ManyToMany(mappedBy = "participants", fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    val chatRooms: MutableSet<ChatRoom> = mutableSetOf(),

    ) {
    /**
     * Equality is based solely on the entity ID.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

/**
 * **User Roles**
 *
 * Defines hierarchical access control levels:
 */
enum class UserRole {
    /** Platform-wide administrator with full permissions. */
    ADMIN,

    /** Project/workspace owner. Full control over owned resources. */
    OWNER,

    /** Collaborator invited to participate in another user's workspace. */
    COLLABORATOR,

    /** Default user role with limited access until invited. */
    USER
}
