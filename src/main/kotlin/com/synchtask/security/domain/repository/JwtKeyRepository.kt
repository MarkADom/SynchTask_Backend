package com.synchtask.security.domain.repository

import com.synchtask.security.domain.entity.JwtKeyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JwtKeyRepository : JpaRepository<JwtKeyEntity, Long> {
    fun findTopByOrderByCreatedAtDesc(): JwtKeyEntity?
}
