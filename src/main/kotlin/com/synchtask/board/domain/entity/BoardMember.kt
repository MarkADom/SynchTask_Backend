package com.synchtask.board.domain.entity

import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import jakarta.persistence.Column
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
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime


@Entity
@Table(
    name = "board_membership",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_board_membership_board_user",
            columnNames = ["board_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_board_membership_board_id", columnList = "board_id"),
        Index(name = "idx_board_membership_user_id", columnList = "user_id")
    ]
)
class BoardMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    val board: Board,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: MembershipRole,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    val createdByUser: User? = null,
)
