package com.synchtask.config

import io.github.bucket4j.Bucket
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RateLimitConfigTest {
    private val userLimit = 100L
    private val userWindow = 1L // 1 minute
    private val anonLimit = 5L
    private val anonWindow = 1L
    private val publicLimit = 500L
    private val publicWindow = 10L

    private val config =
        RateLimitConfig(
            userRequestLimit = userLimit,
            userTimeWindow = userWindow,
            anonRequestLimit = anonLimit,
            anonTimeWindow = anonWindow,
            publicRequestLimit = publicLimit,
            publicTimeWindow = publicWindow
        )

    @Test
    fun `should create bucket for authenticated user`() {
        val identifier = "user@example.com"
        val bucket: Bucket = config.resolveBucket(identifier)

        // Consume one token
        assertTrue(bucket.tryConsume(1))

        // Should be userLimit - 1 remaining
        val remaining = bucket.availableTokens
        assertEquals(userLimit - 1, remaining)
    }

    @Test
    fun `should create stricter bucket for anonymous user`() {
        val identifier = "anon-192.168.0.1"
        val bucket: Bucket = config.resolveBucket(identifier)

        assertTrue(bucket.tryConsume(1))
        val remaining = bucket.availableTokens
        assertEquals(anonLimit - 1, remaining)
    }

    @Test
    fun `should create public bucket with correct configuration`() {
        val publicBucket: Bucket = config.publicBucket()

        assertTrue(publicBucket.tryConsume(1))
        val remaining = publicBucket.availableTokens
        assertEquals(publicLimit - 1, remaining)
    }
}
