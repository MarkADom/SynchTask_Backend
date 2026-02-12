package com.synchtask.task.domain.entity

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
 * Task link entity.
 *
 * Represents an external link associated with a task.
 */
@Entity
@Table(
    name = "task_links",
    indexes = [
        Index(name = "idx_task_link_task", columnList = "task_id")
    ]
)
class TaskLink(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    val task: Task,

    @Column(nullable = false, length = 255)
    val title: String,

    @Column(nullable = false, length = 2048)
    val url: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)


