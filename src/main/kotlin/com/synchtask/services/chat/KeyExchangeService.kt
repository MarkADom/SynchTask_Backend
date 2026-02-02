package com.synchtask.services.chat

import com.synchtask.user.domain.entity.UserEncryptionKeys
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.user.domain.repository.UserEncryptionKeysRepository
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KeyExchangeService(
    private val userEncryptionKeysRepository: UserEncryptionKeysRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(KeyExchangeService::class.java)

    @Transactional
    fun saveUserPublicKey(userEmail: String, publicKey: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val existingKey = userEncryptionKeysRepository.findByUser(user)

        if (existingKey.isPresent) {
            val keyEntry = existingKey.get()
            keyEntry.publicKey = publicKey
            userEncryptionKeysRepository.save(keyEntry)
            logger.info("Updated public key for user: $userEmail")
        } else {
            val newKeyEntry = UserEncryptionKeys(user = user, publicKey = publicKey)
            userEncryptionKeysRepository.save(newKeyEntry)
            logger.info("Saved new public key for user: $userEmail")
        }
    }

    fun getUserPublicKey(userEmail: String): String? {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        return userEncryptionKeysRepository.findByUser(user)
            .map { it.publicKey }
            .orElse(null)
    }

    @Transactional
    fun revokeUserPublicKey(userEmail: String): Boolean {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val keyEntry = userEncryptionKeysRepository.findByUser(user)
        return if (keyEntry.isPresent) {
            userEncryptionKeysRepository.delete(keyEntry.get())
            logger.info("Revoked public key for user: $userEmail")
            true
        } else {
            logger.warn("No public key found to revoke for user: $userEmail")
            false
        }
    }

    @Transactional
    fun rotateUserPublicKey(userEmail: String, newPublicKey: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val keyEntry = userEncryptionKeysRepository.findByUser(user)
        if (keyEntry.isPresent) {
            val existing = keyEntry.get()
            existing.publicKey = newPublicKey
            userEncryptionKeysRepository.save(existing)
            logger.info("Rotated public key for user: $userEmail")
        } else {
            val newEntry = UserEncryptionKeys(user = user, publicKey = newPublicKey)
            userEncryptionKeysRepository.save(newEntry)
            logger.info("Saved new public key during rotation for user: $userEmail")
        }
    }
}
