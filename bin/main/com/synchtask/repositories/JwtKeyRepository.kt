package com.synchtask.repositories

import com.synchtask.entities.JwtKeyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JwtKeyRepository : JpaRepository<JwtKeyEntity, Long> {
    fun findTopByOrderByCreatedAtDesc(): JwtKeyEntity?
}
