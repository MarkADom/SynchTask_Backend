package com.synchtask.entities

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "task_links")
data class TaskLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    val task: Task,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val url: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
