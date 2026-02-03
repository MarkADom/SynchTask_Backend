package com.synchtask.chat.application.service

import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserEncryptionKeys
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.repository.UserEncryptionKeysRepository
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*

class KeyExchangeServiceTest {

    private val userRepository: UserRepository = mockk()
    private val userEncryptionKeysRepository: UserEncryptionKeysRepository = mockk()
    private lateinit var keyExchangeService: KeyExchangeService

    private val user = User(
        id = 1L,
        name = "Alice",
        email = "alice@example.com",
        passwordHash = "hash",
        profilePictureUrl = null
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        keyExchangeService = KeyExchangeService(userEncryptionKeysRepository, userRepository)
    }

    @Test
    fun `should save new user public key`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(user)
        every { userEncryptionKeysRepository.findByUser(user) } returns Optional.empty()
        every { userEncryptionKeysRepository.save(any()) } returnsArgument 0

        keyExchangeService.saveUserPublicKey("alice@example.com", "public-key-123")

        verify { userEncryptionKeysRepository.save(match {
            it.user == user && it.publicKey == "public-key-123"
        }) }
    }

    @Test
    fun `should update existing user public key`() {
        val existingKey = UserEncryptionKeys(id = 10L, user = user, publicKey = "old-key")
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(user)
        every { userEncryptionKeysRepository.findByUser(user) } returns Optional.of(existingKey)
        every { userEncryptionKeysRepository.save(existingKey) } returns existingKey

        keyExchangeService.saveUserPublicKey("alice@example.com", "new-key")

        assertEquals("new-key", existingKey.publicKey)
        verify { userEncryptionKeysRepository.save(existingKey) }
    }

    @Test
    fun `should return user public key`() {
        val key = UserEncryptionKeys(user = user, publicKey = "retrieved-key")
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(user)
        every { userEncryptionKeysRepository.findByUser(user) } returns Optional.of(key)

        val result = keyExchangeService.getUserPublicKey("alice@example.com")

        assertEquals("retrieved-key", result)
    }

    @Test
    fun `should return null if key not found`() {
        every { userRepository.findByEmail("alice@example.com") } returns Optional.of(user)
        every { userEncryptionKeysRepository.findByUser(user) } returns Optional.empty()

        val result = keyExchangeService.getUserPublicKey("alice@example.com")

        assertNull(result)
    }

    @Test
    fun `should throw exception if user not found when saving key`() {
        every { userRepository.findByEmail("ghost@example.com") } returns Optional.empty()

        val exception = assertThrows<ResourceNotFoundException> {
            keyExchangeService.saveUserPublicKey("ghost@example.com", "key")
        }

        assertEquals(
            "User not found: ghost@example.com",
            exception.message
        )
    }


    @Test
    fun `should throw exception if user not found when retrieving key`() {
        every { userRepository.findByEmail("ghost@example.com") } returns Optional.empty()

        val exception = assertThrows<ResourceNotFoundException> {
            keyExchangeService.getUserPublicKey("ghost@example.com")
        }

        assertEquals(
            "User not found: ghost@example.com",
            exception.message
        )
    }
}
