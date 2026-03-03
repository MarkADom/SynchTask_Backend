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
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Chat message entity.
 *
 * Represents a single encrypted message sent inside a chat room.
 */
@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(
            name = "idx_chat_message_room_timestamp",
            columnList = "chat_room_id, timestamp"
        )
    ]
)
class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    val chatRoom: ChatRoom,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,
    @Column(nullable = false, length = 5000)
    val encryptedMessage: String,
    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
