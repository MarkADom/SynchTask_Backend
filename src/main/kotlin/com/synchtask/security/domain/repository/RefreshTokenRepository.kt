package com.synchtask.security.domain.repository

import com.synchtask.security.domain.entity.RefreshToken
import com.synchtask.user.domain.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    @EntityGraph(attributePaths = ["com/synchtask/user"])
    fun findAllByUserAndIsRevokedFalse(user: User): List<RefreshToken>

    @Modifying
    @Transactional
    fun deleteByUser(user: User)

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.token = :token")
    fun revokeByToken(token: String): Int
}
