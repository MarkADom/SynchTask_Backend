package com.synchtask.security.infrastructure.config

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.security.KeyPairGenerator
import kotlin.io.path.writeText
import kotlin.test.assertNotNull

class JwtKeyFileConfigTest {

    @Test
    fun `should load RSA key pair from PEM files`() {
        // Generate real RSA key pair for test
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()

        // Convert keys to PEM format
        val privatePem = toPem(
            keyPair.private.encoded,
            "PRIVATE KEY"
        )
        val publicPem = toPem(
            keyPair.public.encoded,
            "PUBLIC KEY"
        )

        // Create temporary files
        val privateFile = Files.createTempFile("private", ".pem")
        val publicFile = Files.createTempFile("public", ".pem")

        privateFile.writeText(privatePem)
        publicFile.writeText(publicPem)

        // Instantiate config with temp paths
        val config = JwtKeyFileConfig(
            privateKeyPath = privateFile.toString(),
            publicKeyPath = publicFile.toString()
        )

        val loadedKeyPair = config.jwtKeyPair()

        assertNotNull(loadedKeyPair.private)
        assertNotNull(loadedKeyPair.public)
    }

    private fun toPem(encoded: ByteArray, type: String): String {
        val base64 = java.util.Base64.getEncoder().encodeToString(encoded)
        return buildString {
            append("-----BEGIN $type-----\n")
            append(base64.chunked(64).joinToString("\n"))
            append("\n-----END $type-----")
        }
    }
}
