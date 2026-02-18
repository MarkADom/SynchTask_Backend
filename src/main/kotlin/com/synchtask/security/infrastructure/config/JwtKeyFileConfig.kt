package com.synchtask.security.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
@Profile("!test")
class JwtKeyFileConfig {
    @Bean
    fun jwtKeyPair(): KeyPair {
        val privateKey = readPrivateKey(PRIVATE_KEY_PATH)
        val publicKey = readPublicKey(PUBLIC_KEY_PATH)
        return KeyPair(publicKey, privateKey)
    }

    private fun readPrivateKey(path: String): RSAPrivateKey {
        val keyBytes = Files.readAllBytes(Paths.get(path))
        val keySpec = PKCS8EncodedKeySpec(pemToDer(keyBytes))
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun readPublicKey(path: String): RSAPublicKey {
        val keyBytes = Files.readAllBytes(Paths.get(path))
        val keySpec = X509EncodedKeySpec(pemToDer(keyBytes))
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }

    private fun pemToDer(pemBytes: ByteArray): ByteArray {
        val pemString =
            String(pemBytes)
                .replace(Regex("-----BEGIN (.*?)-----"), "")
                .replace(Regex("-----END (.*?)-----"), "")
                .replace(Regex("\\s+"), "")
        return Base64.getDecoder().decode(pemString)
    }

    companion object {
        private const val PRIVATE_KEY_PATH = "config/keys/private.pem"
        private const val PUBLIC_KEY_PATH = "config/keys/public.pem"
    }
}
