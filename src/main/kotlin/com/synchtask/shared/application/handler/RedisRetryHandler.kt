package com.synchtask.shared.application.handler

import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class RedisRetryHandler(
    private val redisTemplate: StringRedisTemplate // Injecting Redis template to retry publishing
) {

    private val logger = LoggerFactory.getLogger(RedisRetryHandler::class.java)
    private val scheduler = Executors.newScheduledThreadPool(1)

    fun retryMessagePublishing(channel: String, message: String, attempt: Int = 1) {
        val delay = calculateBackoffDelay(attempt)

        scheduler.schedule({
            try {
                // Redis publishing retry logic
                redisTemplate.convertAndSend(channel, message)
                logger.info("Successfully published message to Redis channel '$channel' after $attempt attempts.")

            } catch (ex: RedisConnectionFailureException) {
                logger.error("Redis connection failure on attempt $attempt for channel '$channel': ${ex.message}")

                if (attempt < MAX_RETRIES) {
                    retryMessagePublishing(channel, message, attempt + 1)
                } else {
                    logger.error("Redis retry failed after $MAX_RETRIES attempts. Message lost: $message")
                }
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        return (BASE_BACKOFF_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt.toDouble())).toLong()
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val BASE_BACKOFF_DELAY_MS = 1000L // Base delay in milliseconds
        private const val BACKOFF_MULTIPLIER = 2.0 // Exponential factor for retries
    }
}
