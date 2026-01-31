package com.synchtask.entities

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
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime

@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(name = "idx_chat_timestamp", columnList = "timestamp")
    ]
)
class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN) // Reduces multiple queries when loading messages
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "sender_id", nullable = false)
    var sender: User,

    @Column(nullable = false, length = 5000)
    var encryptedMessage: String,

    @Column(nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now(),
)
