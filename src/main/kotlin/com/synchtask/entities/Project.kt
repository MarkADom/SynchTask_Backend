package com.synchtask.entities

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Column(columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = true)
    var tag: String = "",

    @Column(nullable = true)
    var color: String = "#60A5FA",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var dueDate: LocalDate,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_members",
        joinColumns = [JoinColumn(name = "project_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    val members: MutableSet<User> = mutableSetOf(),

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
