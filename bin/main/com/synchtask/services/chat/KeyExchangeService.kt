package com.synchtask.services.chat

import com.synchtask.context.ChatServiceContext
import com.synchtask.dtos.chat.WebSocketMessageDTO
import com.synchtask.entities.ChatMessage
import com.synchtask.entities.UserEncryptionKeys
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.UserEncryptionKeysRepository
import com.synchtask.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * **Key Exchange Service**
 *
 * Manages the secure storage, retrieval, and revocation of user encryption keys for E2E communication.
 */
@Service
class KeyExchangeService(
    private val userEncryptionKeysRepository: UserEncryptionKeysRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(KeyExchangeService::class.java)

    /**
     * Stores or updates the public encryption key for a user.
     *
     * @param userEmail The email of the user.
     * @param publicKey The public key string.
     * @throws ResourceNotFoundException if the user does not exist.
     */
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

    /**
     * Retrieves the public encryption key for a given user.
     *
     * @param userEmail The email of the user whose key is being fetched.
     * @return The public key string if available, otherwise null.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    fun getUserPublicKey(userEmail: String): String? {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        return userEncryptionKeysRepository.findByUser(user)
            .map { it.publicKey }
            .orElse(null)
    }

    /**
     * Revokes a user's public encryption key.
     *
     * @param userEmail The email of the user.
     * @return True if the key was deleted, false if no key was found.
     */
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

    /**
     * Rotates the public key for a user by replacing the existing key with a new one.
     *
     * @param userEmail The email of the user.
     * @param newPublicKey The new public key string.
     */
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
