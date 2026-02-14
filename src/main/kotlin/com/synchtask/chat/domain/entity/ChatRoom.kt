package com.synchtask.chat.domain.entity

import com.synchtask.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * Chat room aggregate root.
 *
 * Represents a conversation between multiple users.
 */
@Entity
@Table(
    name = "chat_rooms",
    indexes = [
        Index(name = "idx_chat_room_created_at", columnList = "created_at")
    ]
)
class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    /**
     * Participants of this chat room.
     * A unique constraint prevents duplicated (chat_room_id, user_id) pairs.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chat_room_participants",
        joinColumns = [JoinColumn(name = "chat_room_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_chat_room_participants_room_user",
                columnNames = ["chat_room_id", "user_id"]
            )
        ]
    )
    var participants: MutableSet<User> = mutableSetOf(),
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
