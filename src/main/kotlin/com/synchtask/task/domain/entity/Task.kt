package com.synchtask.task.domain.entity

import com.synchtask.board.domain.entity.Board
import com.synchtask.task.domain.exception.InvalidTaskStatusTransitionException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * Task aggregate root.
 *
 * Represents a single task inside a board.
 * The database schema is intentionally derived from this entity (ddl-auto: update).
 */
@Entity
@Table(
    name = "tasks",
    indexes = [
        Index(name = "idx_task_status", columnList = "status"),
        Index(name = "idx_task_created_at", columnList = "created_at")
    ]
)
data class Task(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, length = 1000)
    var description: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    /**
     * Users collaborating on this task.
     * A unique constraint prevents duplicated (task_id, user_id) pairs.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_collaborators",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_task_collaborators_task_user",
                columnNames = ["task_id", "user_id"]
            )
        ]
    )
    val collaborators: MutableSet<User> = mutableSetOf(),

    /**
     * Simple labels associated with the task.
     * Stored as an element collection with a unique constraint per task.
     */
    @ElementCollection
    @CollectionTable(
        name = "task_labels",
        joinColumns = [JoinColumn(name = "task_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_task_labels_task_label",
                columnNames = ["task_id", "label"]
            )
        ]
    )
    @Column(name = "label", nullable = false, length = 100)
    var labels: MutableSet<String> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TaskStatus = TaskStatus.TODO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var priority: TaskPriority = TaskPriority.MID,

    @OneToMany(
        mappedBy = "task",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val comments: MutableSet<TaskComment> = mutableSetOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    var board: Board,
) {
    /**
     * Domain invariants
     */
    fun canBeEditedBy(user: User): Boolean {
        return owner.id == user.id || user.role == UserRole.ADMIN
    }

    fun canBeAccessedBy(user: User): Boolean {
        return owner.id == user.id || collaborators.any { it.id == user.id }
    }

    fun canAddCollaborator(requester: User): Boolean {
        return owner.id == requester.id
    }

    fun addCollaborator(user: User): Boolean {
        if (collaborators.any { it.id == user.id }) {
            return false
        }
        collaborators.add(user)
        updatedAt = LocalDateTime.now()
        return true
    }

    fun updateDetails(
        title: String?,
        description: String?,
        labels: Set<String>?,
        status: TaskStatus?,
        priority: TaskPriority?,
    ) {
        title?.let { this.title = it }
        description?.let { this.description = it }
        labels?.let { this.labels = it.toMutableSet() }
        status?.let { changeStatus(it) }
        priority?.let { this.priority = it }
        updatedAt = LocalDateTime.now()
    }

    fun changeStatus(newStatus: TaskStatus) {
        if (!canTransitionTo(newStatus)) {
            throw InvalidTaskStatusTransitionException(
                from = this.status,
                to = newStatus
            )
        }

        this.status = newStatus
        this.updatedAt = LocalDateTime.now()
    }

    private fun canTransitionTo(target: TaskStatus): Boolean {
        val allowedTransitions = mapOf(
            TaskStatus.TODO to setOf(TaskStatus.IN_PROGRESS),
            TaskStatus.IN_PROGRESS to setOf(TaskStatus.REVIEW),
            TaskStatus.REVIEW to setOf(TaskStatus.COMPLETED, TaskStatus.BLOCKED),
            TaskStatus.BLOCKED to setOf(TaskStatus.IN_PROGRESS),
            TaskStatus.COMPLETED to emptySet()
        )

        return allowedTransitions[this.status]?.contains(target) ?: false
    }
}

/**
 * Task lifecycle status.
 */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    COMPLETED,
    BLOCKED
}

/**
 * Task priority level.
 */
enum class TaskPriority {
    LOW,
    MID,
    HIGH
}
