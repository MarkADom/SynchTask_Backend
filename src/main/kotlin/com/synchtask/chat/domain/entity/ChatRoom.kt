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
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime

@Entity
@Table(
    name = "chat_rooms",
    indexes = [
        Index(name = "idx_chatroom_created_at", columnList = "created_at")
    ]
)
class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(
        name = "chat_room_participants",
        joinColumns = [JoinColumn(name = "chat_room_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var participants: MutableSet<User> = mutableSetOf(),

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
