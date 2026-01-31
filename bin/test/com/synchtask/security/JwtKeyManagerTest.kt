package com.synchtask.security

import com.synchtask.entities.JwtKeyEntity
import com.synchtask.repositories.JwtKeyRepository
import io.mockk.*
import org.junit.jupiter.api.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.security.PrivateKey
import java.util.*
import kotlin.test.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtKeyManagerTest {

    private lateinit var keyRepository: JwtKeyRepository
    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var valueOps: ValueOperations<String, String>
    private lateinit var manager: JwtKeyManager

    @BeforeEach
    fun setUp() {
        keyRepository = mockk()
        redisTemplate = mockk()
        valueOps = mockk()
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should return true for isRSA`() {
        manager = buildManagerWithoutKeys()
        assertTrue(manager.isRSA())
    }

    @Test
    fun `should load existing key pair from DB and Redis`() {
        val keyPair = generateKeyPair()
        val privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        every { keyRepository.findTopByOrderByCreatedAtDesc() } returns JwtKeyEntity(privateKey = privateKeyBase64)
        every { valueOps.get("jwt:publicKey") } returns publicKeyBase64
        every { keyRepository.save(any()) } returnsArgument 0
        every { valueOps.set(any(), any()) } just Runs

        manager = JwtKeyManager(keyRepository, redisTemplate)

        assertNotNull(manager.getPrivateKey())
        assertNotNull(manager.getPublicKey())
    }

    @Test
    fun `should generate and store key pair if DB and Redis are empty`() {
        every { keyRepository.findTopByOrderByCreatedAtDesc() } returns null
        every { valueOps.set(any(), any()) } just Runs
        every { valueOps.get("jwt:publicKey") } returns null
        every { keyRepository.save(any()) } returnsArgument 0

        manager = JwtKeyManager(keyRepository, redisTemplate)

        assertNotNull(manager.getPrivateKey())
        assertNotNull(manager.getPublicKey())
    }


    @Test
    fun `should fallback and regenerate key if public key in Redis is invalid`() {
        val keyPair = generateKeyPair()
        val privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        val invalidButBase64Key = Base64.getEncoder().encodeToString("invalid-key-format".toByteArray())

        every { keyRepository.findTopByOrderByCreatedAtDesc() } returns JwtKeyEntity(privateKey = privateKeyBase64)
        every { valueOps.get("jwt:publicKey") } returns invalidButBase64Key
        every { valueOps.set(any(), any()) } just Runs
        every { keyRepository.save(any()) } returnsArgument 0

        manager = JwtKeyManager(keyRepository, redisTemplate)

        assertNotNull(manager.getPublicKey())
    }


    @Test
    fun `should return public key in JWKS format`() {
        manager = buildManagerWithoutKeys()

        val jwks = manager.getJwks()
        val keys = jwks["keys"] as? List<*>
        val key = keys?.first() as? Map<*, *>

        assertEquals("RSA", key?.get("kty"))
        assertEquals("RS256", key?.get("alg"))
        assertEquals("sig", key?.get("use"))
        assertTrue(key?.get("n") is String)
        assertTrue(key?.get("e") is String)
    }

    @Test
    fun `should rotate keys and update reference`() {
        every { keyRepository.save(any()) } returnsArgument 0
        every { valueOps.set(any(), any()) } just Runs
        manager = buildManagerWithoutKeys()

        val oldPrivateKey: PrivateKey = manager.getPrivateKey()
        manager.rotateKeys()
        val newPrivateKey: PrivateKey = manager.getPrivateKey()

        assertNotEquals(oldPrivateKey, newPrivateKey)
    }

    /**
     * **Helpers**
     */
    
    private fun buildManagerWithoutKeys(): JwtKeyManager {
        every { keyRepository.findTopByOrderByCreatedAtDesc() } returns null
        every { keyRepository.save(any()) } returnsArgument 0
        every { valueOps.set(any(), any()) } just Runs
        every { valueOps.get("jwt:publicKey") } returns null
        return JwtKeyManager(keyRepository, redisTemplate)
    }

    private fun generateKeyPair(): java.security.KeyPair {
        val generator = java.security.KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }
}
