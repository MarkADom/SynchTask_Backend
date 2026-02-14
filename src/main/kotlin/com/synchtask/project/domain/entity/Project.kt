package com.synchtask.project.domain.entity

import com.synchtask.board.domain.entity.Board
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Project aggregate root.
 *
 * A project groups multiple boards and members under a single owner.
 */
@Entity
@Table(
    name = "projects",
    indexes = [
        Index(name = "idx_project_owner_id", columnList = "owner_id"),
        Index(name = "idx_project_name", columnList = "name"),
        Index(name = "idx_project_created_at", columnList = "created_at")
    ]
)
class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = "",
    @Column(nullable = false)
    var tag: String = "",
    @Column(nullable = false)
    var color: String = "#60A5FA",
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = null,
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,
    /**
     * Users that are members of this project.
     * A unique constraint prevents duplicated (project_id, user_id) pairs.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_members",
        joinColumns = [JoinColumn(name = "project_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_project_members_project_user",
                columnNames = ["project_id", "user_id"]
            )
        ]
    )
    val members: MutableSet<User> = mutableSetOf(),
    /**
     * Boards that belong to this project.
     * Orphan removal ensures consistency when boards are removed.
     */
    @OneToMany(
        mappedBy = "project",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val boards: MutableSet<Board> = mutableSetOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Project) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
