package com.synchtask.task.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "task_attachments")
data class TaskAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    val task: Task,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val fileUrl: String,

    @Column(nullable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)
