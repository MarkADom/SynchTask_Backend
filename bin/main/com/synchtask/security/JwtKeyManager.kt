package com.synchtask.security

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

/**
 * **JWT Key Manager (PEM Version)**
 *
 * - Loads RSA keys securely from local `.pem` files.
 * - Provides access to private and public keys for JWT operations.
 * - Designed for high security, flexibility, and minimal complexity.
 */
@Component
class JwtKeyManager {

    private val logger = LoggerFactory.getLogger(JwtKeyManager::class.java)

    private val keyPairRef = AtomicReference(loadKeyPair())

    /**
     * Loads the RSA Key Pair from configured `.pem` files.
     */
    private fun loadKeyPair(): KeyPair {
        return try {
            val privateKey = readPrivateKey(PRIVATE_KEY_PATH)
            val publicKey = readPublicKey(PUBLIC_KEY_PATH)

            logger.info("Successfully loaded RSA keys from PEM files.")
            KeyPair(publicKey, privateKey)
        } catch (ex: Exception) {
            logger.error("Failed to load RSA keys from PEM files.", ex)
            throw IllegalStateException("Unable to load RSA key pair.", ex)
        }
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
        val pemString = String(pemBytes)
            .replace(Regex("-----BEGIN (.*?)-----"), "")
            .replace(Regex("-----END (.*?)-----"), "")
            .replace(Regex("\\s+"), "")
        return Base64.getDecoder().decode(pemString)
    }

    fun isRSA(): Boolean = true

    fun getPrivateKey(): PrivateKey = keyPairRef.get().private

    fun getPublicKey(): PublicKey = keyPairRef.get().public

    fun rotateKeys() {
        throw UnsupportedOperationException("Manual rotation not supported with file-based keys. Update the PEM files manually.")
    }

    /**
     * **Returns the Public Key in JWKS (JSON Web Key Set) format with a Key ID (kid).**
     *
     * @return Map containing the JWKS structure.
     */
    fun getJwks(): Map<String, Any> {
        val publicKey = getPublicKey() as RSAPublicKey

        val modulusBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray())
        val exponentBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray())

        val kid = generateKid(publicKey)

        return mapOf(
            "keys" to listOf(
                mapOf(
                    "kty" to "RSA",           // Key Type
                    "alg" to "RS256",         // Algorithm
                    "use" to "sig",           // Usage: Signature
                    "n" to modulusBase64Url,  // Modulus
                    "e" to exponentBase64Url, // Exponent
                    "kid" to kid              // Key ID (automatically generated)
                )
            )
        )
    }

    /**
     * **Generates a Key ID (kid) based on the SHA-256 hash of the public key.**
     *
     * @param publicKey RSAPublicKey to generate a kid for.
     * @return Base64 URL-encoded kid.
     */
    private fun generateKid(publicKey: RSAPublicKey): String {
        val publicKeyBytes = publicKey.encoded
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(publicKeyBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    /**
     * Calculates a unique Key ID (kid) based on SHA-256 of the modulus.
     */
    private fun calculateKid(publicKey: RSAPublicKey): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(publicKey.modulus.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    private fun BigInteger.toByteArray(): ByteArray {
        val array = this.toByteArray()
        return if (array[0] == 0.toByte()) array.copyOfRange(1, array.size) else array
    }

    companion object {
        private const val PRIVATE_KEY_PATH = "config/keys/private.pem"
        private const val PUBLIC_KEY_PATH = "config/keys/public.pem"
    }
}
