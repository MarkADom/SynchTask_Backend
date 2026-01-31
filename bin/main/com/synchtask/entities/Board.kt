package com.synchtask.entities

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * **Board Entity**
 *
 * Represents a Kanban-style board that belongs to a specific user.
 * Each board may contain multiple tasks and can be customized by its owner.
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

    @Column(nullable = true)
    var color: String? = null,

    @Column(nullable = true)
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "board_collaborators",
        joinColumns = [JoinColumn(name = "board_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    val collaborators: MutableSet<User> = mutableSetOf(),

    @OneToMany(
        mappedBy = "board",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val tasks: MutableSet<Task> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    var project: Project? = null
) {

    /**
     * Checks if the given user is the owner of this board.
     */
    fun isOwnedBy(user: User): Boolean = owner.id == user.id

    /**
     * Verifies whether the given user has access to this board.
     * Access is granted to the board's owner or any listed collaborator.
     *
     * @param user The user to check access for.
     * @return True if the user can access, otherwise false.
     */
    fun hasAccess(user: User): Boolean {
        return owner.id == user.id || collaborators.any { it.id == user.id }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
