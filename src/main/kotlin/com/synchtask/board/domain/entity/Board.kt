package com.synchtask.board.domain.entity

import com.synchtask.board.application.dto.BoardUpdateDTO
import com.synchtask.project.domain.entity.Project
import com.synchtask.task.domain.entity.Task
import com.synchtask.user.domain.entity.User
import jakarta.persistence.CascadeType
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
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * Board aggregate root.
 *
 * A board groups tasks and collaborators and belongs to a single owner.
 */
@Entity
@Table(
    name = "boards",
    indexes = [
        Index(name = "idx_board_owner_id", columnList = "owner_id"),
        Index(name = "idx_board_name", columnList = "name"),
        Index(name = "idx_board_created_at", columnList = "created_at")
    ]
)
class Board(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var color: String = "#60A5FA",

    @Column(nullable = false, length = 1000)
    var description: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = null,

    /**
     * Users collaborating on this board.
     * A unique constraint prevents duplicated (board_id, user_id) pairs.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "board_collaborators",
        joinColumns = [JoinColumn(name = "board_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_board_collaborators_board_user",
                columnNames = ["board_id", "user_id"]
            )
        ]
    )
    val collaborators: MutableSet<User> = mutableSetOf(),

    /**
     * Tasks that belong to this board.
     * Orphan removal ensures consistency when tasks are deleted.
     */
    @OneToMany(
        mappedBy = "board",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val tasks: MutableSet<Task> = mutableSetOf(),

    /**
     * Optional project that groups this board.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    var project: Project? = null
) {
    fun updateFrom(dto: BoardUpdateDTO) {
        dto.name?.let { name = it }
        dto.color?.let { color = it }
        dto.description?.let { description = it }
        updatedAt = LocalDateTime.now()
    }

    fun isOwnedBy(user: User): Boolean =
        owner.id == user.id

    fun hasAccess(user: User): Boolean =
        owner.id == user.id || collaborators.any { it.id == user.id }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        return id == other.id
    }

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}
