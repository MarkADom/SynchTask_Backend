package com.synchtask.security.infrastructure.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.security.KeyPair
import java.security.KeyPairGenerator

@TestConfiguration
@Profile("test")
class JwtTestKeyConfig {
    @Bean
    fun jwtKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }
}
