package com.synchtask.repositories

import com.synchtask.entities.User
import com.synchtask.entities.UserEncryptionKeys
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * **User Encryption Keys Repository**
 *
 * Handles database operations related to user encryption keys.
 */
@Repository
interface UserEncryptionKeysRepository : JpaRepository<UserEncryptionKeys, Long> {
    fun findByUser(user: User): Optional<UserEncryptionKeys>
}
