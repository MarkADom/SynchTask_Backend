package com.synchtask.services.redis

import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.data.redis.core.StringRedisTemplate

class RedisPublisherTest {

    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var redisPublisher: RedisPublisher
    private val sleeper: (Long) -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        redisTemplate = mockk()
        redisPublisher = RedisPublisher(redisTemplate, sleeper)
    }

    @Test
    fun `should publish message successfully without retries`() {
        every { redisTemplate.convertAndSend("channel", "message") } returns 1L

        redisPublisher.publish("channel", "message")

        verify(exactly = 1) { redisTemplate.convertAndSend("channel", "message") }
        confirmVerified(redisTemplate)
    }

    @Test
    fun `should retry on RedisConnectionException then succeed`() {
        every { redisTemplate.convertAndSend("channel", "message") } throws RedisConnectionException("Connection lost") andThen 1L
        every { sleeper(any()) } just Runs

        redisPublisher.publish("channel", "message")

        verify(exactly = 2) { redisTemplate.convertAndSend("channel", "message") }
        verify(exactly = 1) { sleeper(any()) }
    }

    @Test
    fun `should not retry on RedisCommandTimeoutException`() {
        every { redisTemplate.convertAndSend("channel", "message") } throws RedisCommandTimeoutException("Timeout")

        redisPublisher.publish("channel", "message")

        verify(exactly = 1) { redisTemplate.convertAndSend("channel", "message") }
        verify(exactly = 0) { sleeper(any()) }
    }

    @Test
    fun `should not retry on DataAccessException`() {
        every { redisTemplate.convertAndSend("channel", "message") } throws InvalidDataAccessResourceUsageException("Simulated")

        redisPublisher.publish("channel", "message")

        verify(exactly = 1) { redisTemplate.convertAndSend("channel", "message") }
        verify(exactly = 0) { sleeper(any()) }
    }

    @Test
    fun `should not retry on IllegalStateException`() {
        every { redisTemplate.convertAndSend("channel", "message") } throws IllegalStateException("Illegal state")

        redisPublisher.publish("channel", "message")

        verify(exactly = 1) { redisTemplate.convertAndSend("channel", "message") }
        verify(exactly = 0) { sleeper(any()) }
    }

    @Test
    fun `should not retry on unknown exception`() {
        every { redisTemplate.convertAndSend("channel", "message") } throws RuntimeException("Generic error")

        redisPublisher.publish("channel", "message")

        verify(exactly = 1) { redisTemplate.convertAndSend("channel", "message") }
        verify(exactly = 0) { sleeper(any()) }
    }
}
