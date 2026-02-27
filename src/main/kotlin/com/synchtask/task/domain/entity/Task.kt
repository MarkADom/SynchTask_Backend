package com.synchtask.task.domain.entity

import com.synchtask.board.domain.entity.Board
import com.synchtask.shared.domain.legacy.Legacy
import com.synchtask.shared.exception.LegacyPathInvokedException
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
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.slf4j.LoggerFactory
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
class Task(
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

    @OneToMany(
        mappedBy = "task",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val members: MutableSet<TaskMember> = mutableSetOf(),
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
        if (user.role == UserRole.ADMIN) return true

        return members.any { it.user.id == user.id }
    }

    fun canBeAccessedBy(user: User): Boolean {
        if (user.role == UserRole.ADMIN) return true
        val taskMembership = members.any { it.user.id == user.id }
        val boardMembership = board.members.any { it.user.id == user.id }

        return taskMembership || boardMembership
    }

    /**
     * Legacy compatibility shim kept only to fail fast when old join-table collaborator flows are invoked.
     * This exists to detect stale callers while assignee management is now handled via TaskMember/TaskMemberRepository.
     * Removal planned for v1.1.0.
     */
    @Legacy
    fun canAddCollaborator(requester: User): Boolean {
        legacyLogger.warn("Legacy Task.canAddCollaborator invoked for taskId={}, requesterId={}", id, requester.id)
        throw LegacyPathInvokedException(
            "Legacy collaborator path exists for backward compatibility, replaced by TaskMember-based assignments. Removal planned for v1.1.0"
        )
    }

    /**
     * Legacy compatibility shim kept only to fail fast when old join-table collaborator writes are invoked.
     * This exists because runtime membership persistence is now exclusively handled by TaskMemberRepository.
     * Removal planned for v1.1.0.
     */

    @Legacy
    fun addCollaborator(user: User): Boolean {
        legacyLogger.warn("Legacy Task.addCollaborator invoked for taskId={}, userId={}", id, user.id)
        throw LegacyPathInvokedException(
            "Legacy collaborator path exists for backward compatibility, replaced by TaskMemberRepository writes. Removal planned for v1.1.0"
        )
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
        val allowedTransitions =
            mapOf(
                TaskStatus.TODO to setOf(TaskStatus.IN_PROGRESS),
                TaskStatus.IN_PROGRESS to setOf(TaskStatus.REVIEW),
                TaskStatus.REVIEW to setOf(TaskStatus.COMPLETED, TaskStatus.BLOCKED),
                TaskStatus.BLOCKED to setOf(TaskStatus.IN_PROGRESS),
                TaskStatus.COMPLETED to emptySet()
            )

        return allowedTransitions[this.status]?.contains(target) ?: false
    }


    companion object {
        private val legacyLogger = LoggerFactory.getLogger(Task::class.java)
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
