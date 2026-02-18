package com.synchtask.security.infrastructure.config

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.assertNotNull

class JwtKeyFileConfigTest {

    @Test
    fun `should load RSA key pair from PEM files`() {

        // Generate RSA key pair in memory
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val originalKeyPair = generator.generateKeyPair()

        // Convert to PEM format
        val privatePem = toPem(
            "PRIVATE KEY",
            originalKeyPair.private.encoded
        )
        val publicPem = toPem(
            "PUBLIC KEY",
            originalKeyPair.public.encoded
        )

        // Write to temp files
        val privateFile = Files.createTempFile("private", ".pem")
        val publicFile = Files.createTempFile("public", ".pem")

        Files.writeString(privateFile, privatePem)
        Files.writeString(publicFile, publicPem)

        // Instantiate config with injected paths
        val config = JwtKeyFileConfig(
            privateKeyPath = privateFile.toString(),
            publicKeyPath = publicFile.toString()
        )

        val loadedKeyPair = config.jwtKeyPair()

        assertNotNull(loadedKeyPair.private)
        assertNotNull(loadedKeyPair.public)
    }

    private fun toPem(type: String, bytes: ByteArray): String {
        val base64 = Base64.getEncoder().encodeToString(bytes)
        return """
            -----BEGIN $type-----
            $base64
            -----END $type-----
        """.trimIndent()
    }
}
