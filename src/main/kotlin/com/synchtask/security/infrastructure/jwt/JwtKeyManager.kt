package com.synchtask.security.infrastructure.jwt

import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
* Exposes RSA keys for JWT signing/validation and JWKS generation.
*/
@Component
class JwtKeyManager(
    private val keyPair: KeyPair
) {

    fun isRSA(): Boolean = true

    fun getPrivateKey(): PrivateKey = keyPair.private

    fun getPublicKey(): PublicKey = keyPair.public

    fun rotateKeys() {
        throw UnsupportedOperationException(
            "Manual rotation not supported with file-based keys. Update the PEM files manually."
        )
    }

    fun getJwks(): Map<String, Any> {
        val publicKey = getPublicKey() as RSAPublicKey

        val modulusBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray())
        val exponentBase64Url =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                publicKey.publicExponent.toByteArray()
            )

        val kid = generateKid(publicKey)

        return mapOf(
            "keys" to
                listOf(
                    mapOf(
                        "kty" to "RSA",
                        "alg" to "RS256",
                        "use" to "sig",
                        "n" to modulusBase64Url,
                        "e" to exponentBase64Url,
                        "kid" to kid
                    )
                )
        )
    }

    private fun generateKid(publicKey: RSAPublicKey): String {
        val publicKeyBytes = publicKey.encoded
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(publicKeyBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    private fun calculateKid(publicKey: RSAPublicKey): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(publicKey.modulus.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    private fun BigInteger.toByteArray(): ByteArray {
        val array = this.toByteArray()
        return if (array[0] == 0.toByte()) array.copyOfRange(1, array.size) else array
    }
}
