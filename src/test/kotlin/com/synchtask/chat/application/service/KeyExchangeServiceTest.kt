package com.synchtask.chat.application.service

import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserEncryptionKeys
import com.synchtask.user.domain.repository.UserEncryptionKeysRepository
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.*

class KeyExchangeServiceTest {
    private lateinit var keysRepository: UserEncryptionKeysRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: KeyExchangeService

    private val userEmail = "user@test.com"
    private val user =
        User(
            id = 1L,
            name = "Test User",
            email = userEmail,
            passwordHash = "pw"
        )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        keysRepository = mockk()
        userRepository = mockk()
        service = KeyExchangeService(keysRepository, userRepository)

        every { keysRepository.delete(any()) } just Runs
    }

    @Test
    fun `should save new public key when none exists`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.empty()
        every { keysRepository.save(any()) } answers { firstArg() }

        service.saveUserPublicKey(userEmail, "public-key")

        verify(exactly = 1) {
            keysRepository.save(
                match { it.user == user && it.publicKey == "public-key" }
            )
        }
    }

    @Test
    fun `should replace existing public key when saving`() {
        val existingKey = UserEncryptionKeys(user = user, publicKey = "old-key")

        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.of(existingKey)
        every { keysRepository.save(any()) } answers { firstArg() }

        service.saveUserPublicKey(userEmail, "new-key")

        verifyOrder {
            keysRepository.delete(existingKey)
            keysRepository.save(
                match {
                    it.user == user && it.publicKey == "new-key"
                }
            )
        }
    }

    @Test
    fun `should throw when saving key for non existing user`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.saveUserPublicKey(userEmail, "key")
        }
    }

    @Test
    fun `should return public key when exists`() {
        val key = UserEncryptionKeys(user = user, publicKey = "public-key")

        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.of(key)

        val result = service.getUserPublicKey(userEmail)

        assertEquals("public-key", result)
    }

    @Test
    fun `should return null when public key does not exist`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.empty()

        val result = service.getUserPublicKey(userEmail)

        assertNull(result)
    }

    @Test
    fun `should revoke public key when key exists`() {
        val key = UserEncryptionKeys(user = user, publicKey = "key")

        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.of(key)
        every { keysRepository.delete(key) } just Runs

        val result = service.revokeUserPublicKey(userEmail)

        assertTrue(result)
        verify(exactly = 1) { keysRepository.delete(key) }
    }

    @Test
    fun `should return false when no public key exists to revoke`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.empty()

        val result = service.revokeUserPublicKey(userEmail)

        assertFalse(result)
        verify(exactly = 0) { keysRepository.delete(any()) }
    }

    @Test
    fun `should throw when revoking key for non existing user`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.revokeUserPublicKey(userEmail)
        }
    }

    @Test
    fun `should rotate existing public key by replacing it`() {
        val existingKey = UserEncryptionKeys(user = user, publicKey = "old-key")

        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.of(existingKey)
        every { keysRepository.save(any()) } answers { firstArg() }

        service.rotateUserPublicKey(userEmail, "new-key")

        verifyOrder {
            keysRepository.delete(existingKey)
            keysRepository.save(
                match {
                    it.user == user && it.publicKey == "new-key"
                }
            )
        }
    }

    @Test
    fun `should create new public key when rotating without existing key`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.of(user)
        every { keysRepository.findByUser(user) } returns Optional.empty()
        every { keysRepository.save(any()) } answers { firstArg() }

        service.rotateUserPublicKey(userEmail, "new-key")

        verify(exactly = 1) {
            keysRepository.save(
                match { it.user == user && it.publicKey == "new-key" }
            )
        }
    }

    @Test
    fun `should throw when rotating key for non existing user`() {
        every { userRepository.findByEmail(userEmail) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.rotateUserPublicKey(userEmail, "new-key")
        }
    }
}
