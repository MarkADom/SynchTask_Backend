package com.synchtask.security.infrastructure.config

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwtKeyFileConfigTest {

    @Test
    fun `should load RSA key pair from PEM files`() {
        // Generate real RSA key pair
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val originalKeyPair = keyGen.generateKeyPair()

        val privatePem = toPrivatePem(originalKeyPair.private.encoded)
        val publicPem = toPublicPem(originalKeyPair.public.encoded)

        val privateFile = createTempFile("private", ".pem")
        val publicFile = createTempFile("public", ".pem")

        Files.write(privateFile.toPath(), privatePem.toByteArray())
        Files.write(publicFile.toPath(), publicPem.toByteArray())

        val config = JwtKeyFileConfig(
            privateFile.absolutePath,
            publicFile.absolutePath
        )

        val loadedKeyPair = config.jwtKeyPair()

        assertNotNull(loadedKeyPair.private)
        assertNotNull(loadedKeyPair.public)

        assertEquals(
            (originalKeyPair.private as RSAPrivateKey).modulus,
            (loadedKeyPair.private as RSAPrivateKey).modulus
        )

        assertEquals(
            (originalKeyPair.public as RSAPublicKey).modulus,
            (loadedKeyPair.public as RSAPublicKey).modulus
        )

        privateFile.delete()
        publicFile.delete()
    }

    private fun toPrivatePem(derBytes: ByteArray): String =
        buildString {
            appendLine("-----BEGIN PRIVATE KEY-----")
            appendLine(Base64.getEncoder().encodeToString(derBytes))
            appendLine("-----END PRIVATE KEY-----")
        }

    private fun toPublicPem(derBytes: ByteArray): String =
        buildString {
            appendLine("-----BEGIN PUBLIC KEY-----")
            appendLine(Base64.getEncoder().encodeToString(derBytes))
            appendLine("-----END PUBLIC KEY-----")
        }
}
