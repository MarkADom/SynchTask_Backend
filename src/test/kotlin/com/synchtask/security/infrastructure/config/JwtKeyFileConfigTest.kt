package com.synchtask.security.infrastructure.config

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class JwtKeyFileConfigTest {

    @Test
    fun `should load RSA key pair from PEM files`() {

        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val original = keyGen.generateKeyPair()

        val privateFile = File.createTempFile("private", ".pem")
        val publicFile = File.createTempFile("public", ".pem")

        privateFile.writeText(createPem("PRIVATE KEY", original.private.encoded))
        publicFile.writeText(createPem("PUBLIC KEY", original.public.encoded))

        val config = JwtKeyFileConfig(
            privateFile.absolutePath,
            publicFile.absolutePath
        )

        val loaded = config.jwtKeyPair()

        assertEquals(original.public.algorithm, loaded.public.algorithm)
        assertEquals(original.private.algorithm, loaded.private.algorithm)
    }

    private fun createPem(type: String, bytes: ByteArray): String {
        val base64 = Base64.getEncoder().encodeToString(bytes)
        return """
            -----BEGIN $type-----
            $base64
            -----END $type-----
        """.trimIndent()
    }
}
