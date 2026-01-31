package com.synchtask.repositories

import com.synchtask.entities.RefreshToken
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*


/**
 * **Refresh Token Repository**
 *
 * Provides database access for managing refresh tokens.
 */
@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    /**
     * **Finds a refresh token by its token string.**
     */
    fun findByToken(token: String): Optional<RefreshToken>


    /**
     * **Finds all active (not revoked) refresh tokens belonging to a user.**
     * Uses `@EntityGraph` to load the associated user, avoiding lazy loading issues.
     */
    @EntityGraph(attributePaths = ["user"])
    fun findAllByUserAndIsRevokedFalse(user: User): List<RefreshToken>

    /**
     * **Deletes all refresh tokens associated with a user.**
     * Uses `@Modifying` anf `@Transactional` to avoid Hibernate state problems
     */
    @Modifying
    @Transactional
    fun deleteByUser(user: User)


    /**
     * **Revokes a specific refresh token by marking it as revoked.**
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.token = :token")
    fun revokeByToken(token: String): Int
}
