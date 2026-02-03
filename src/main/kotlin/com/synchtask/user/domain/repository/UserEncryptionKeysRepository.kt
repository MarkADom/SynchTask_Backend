package com.synchtask.user.domain.repository

import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserEncryptionKeys
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEncryptionKeysRepository : JpaRepository<UserEncryptionKeys, Long> {
    fun findByUser(user: User): Optional<UserEncryptionKeys>
}
