package com.synchtask.security.infrastructure.jwt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import kotlin.collections.get

class JwtKeyManagerTest {
    @Test
    fun `should expose injected RSA key pair`() {
        val manager = JwtKeyManager(TestKeyPairs.generateRsa())

        val privateKey: PrivateKey = manager.getPrivateKey()
        val publicKey: PublicKey = manager.getPublicKey()

        assertNotNull(privateKey)
        assertNotNull(publicKey)
    }

    @Test
    fun `should report RSA algorithm`() {
        val manager = JwtKeyManager(TestKeyPairs.generateRsa())
        assertTrue(manager.isRSA())
    }

    @Test
    fun `should expose public key as RSAPublicKey`() {
        val manager = JwtKeyManager(TestKeyPairs.generateRsa())
        val publicKey = manager.getPublicKey()

        assertTrue(publicKey is RSAPublicKey)
    }

    @Test
    fun `should return JWKS with valid structure`() {
        val manager = JwtKeyManager(TestKeyPairs.generateRsa())

        val jwks = manager.getJwks()

        assertTrue(jwks.containsKey("keys"))

        val keys = jwks["keys"] as List<*>
        assertEquals(1, keys.size)

        val key = keys.first() as Map<*, *>

        assertEquals("RSA", key["kty"])
        assertEquals("RS256", key["alg"])
        assertEquals("sig", key["use"])

        assertTrue(key["n"] is String)
        assertTrue(key["e"] is String)
        assertTrue(key["kid"] is String)
    }

    @Test
    fun `should throw when rotating keys`() {
        val manager = JwtKeyManager(TestKeyPairs.generateRsa())

        val ex =
            assertThrows(UnsupportedOperationException::class.java) {
                manager.rotateKeys()
            }

        assertTrue(ex.message!!.contains("Manual rotation not supported"))
    }
}
