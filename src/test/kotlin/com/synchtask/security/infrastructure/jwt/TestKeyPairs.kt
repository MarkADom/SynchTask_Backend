package com.synchtask.security.infrastructure.jwt

import java.security.KeyPair
import java.security.KeyPairGenerator

object TestKeyPairs {
    fun generateRsa(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }
}
