package com.synchtask.user.domain.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.synchtask.chat.domain.entity.ChatRoom
import com.synchtask.entities.Friend
import com.synchtask.notification.domain.entity.Notification
import jakarta.persistence.CascadeType
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
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class UserRole {
    ADMIN,
    OWNER,
    COLLABORATOR,
    USER
}
