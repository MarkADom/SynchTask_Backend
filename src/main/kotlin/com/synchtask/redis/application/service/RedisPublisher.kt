package com.synchtask.redis.application.service

import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import kotlin.math.min
import kotlin.math.pow

@Service
class RedisPublisher(
    private val redisTemplate: StringRedisTemplate,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) {
    private val logger = LoggerFactory.getLogger(RedisPublisher::class.java)

    fun publish(channel: String, message: String) {
        var attempts = 0
        var success = false
        var shouldRetry = true

        while (attempts < MAX_RETRIES && !success && shouldRetry) {
            try {
                redisTemplate.convertAndSend(channel, message)
                logger.info("Message published to Redis channel [{}]: {}", channel, message)
                success = true
            } catch (ex: Exception) {
                attempts++
                shouldRetry = handleRetry(ex, attempts)
            }
        }

        if (!success) {
            logger.error("Failed to publish message to Redis after {} attempts: {}", MAX_RETRIES, message)
        }
    }

    private fun handleRetry(ex: Exception, attempts: Int): Boolean {
        val delay = calculateBackoff(attempts)

        return when (ex) {
            is RedisConnectionException -> {
                logger.warn(
                    "Redis connection failed. Attempt {}/{}. Retrying in {}ms...",
                    attempts,
                    MAX_RETRIES,
                    delay,
                    ex
                )
                sleeper(delay)
                true
            }

            is RedisCommandTimeoutException -> {
                logger.error("Redis command timed out while publishing message.", ex)
                false
            }

            is DataAccessException -> {
                logger.error("Database access error while interacting with Redis.", ex)
                false
            }

            is IllegalStateException -> {
                logger.error("Unexpected system error while publishing to Redis.", ex)
                false
            }

            else -> {
                logger.error("Unknown error occurred.", ex)
                false
            }
        }
    }

    private fun calculateBackoff(attempts: Int): Long {
        val baseDelay = INITIAL_DELAY_MS * (2.0.pow(attempts)).toLong()
        return min(baseDelay, MAX_DELAY_MS)
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L // 1 second
        private const val MAX_DELAY_MS = 5000L // 5 seconds
    }
}
