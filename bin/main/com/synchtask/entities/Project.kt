package com.synchtask.entities

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * **Project Entity**
 *
 * Represents a logical group of boards with common goals or context.
 * Projects are owned by users and can have collaborators.
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

    /** Primary key (auto-generated). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** Project name. */
    @Column(nullable = false)
    var name: String,

    /** Optional short description. */
    @Column(columnDefinition = "TEXT")
    var description: String,

    /** Optional tag or category label. */
    @Column(nullable = true)
    var tag: String = "",

    /** Optional visual color. */
    @Column(nullable = true)
    var color: String = "#60A5FA",

    /** Project owner (creator). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    /** Timestamps */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var dueDate: LocalDate,

    /** Members allowed to collaborate on this project. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_members",
        joinColumns = [JoinColumn(name = "project_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    val members: MutableSet<User> = mutableSetOf(),

    /** Boards grouped under this project. */
    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val boards: MutableSet<Board> = mutableSetOf(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Project) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
