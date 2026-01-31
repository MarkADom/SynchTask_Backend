package com.synchtask.services.notification

import com.synchtask.dtos.notification.NotificationRedisDTO
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

/**
 * **Notification Redis Cleanup Service**
 *
 * Periodically removes expired fields from Redis Hash caches.
 */
@Service
class NotificationRedisCleanupService(
    private val redisTemplate: RedisTemplate<String, NotificationRedisDTO>
) {

    private val logger = LoggerFactory.getLogger(NotificationRedisCleanupService::class.java)

    companion object {
        private val EXPIRATION_THRESHOLD: Duration = Duration.ofHours(24)
    }

    /**
     * Runs every hour to remove expired notification fields from Redis Hashes.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    fun cleanOldNotifications() {
        val keys = redisTemplate.keys("notifications:*")

        if (keys.isEmpty()) {
            logger.debug("No Redis hash keys found for cleanup.")
            return
        }

        val now = LocalDateTime.now()

        keys.forEach { redisKey ->
            val entries = redisTemplate.opsForHash<String, NotificationRedisDTO>().entries(redisKey)
            val expiredFields = entries.filter { (_, value) ->
                Duration.between(value.createdAt, now) > EXPIRATION_THRESHOLD
            }.map { it.key }

            if (expiredFields.isNotEmpty()) {
                redisTemplate.opsForHash<String, NotificationRedisDTO>().delete(redisKey, *expiredFields.toTypedArray())
                logger.info("Cleaned ${expiredFields.size} expired notification(s) from Redis hash: $redisKey")
            }
        }
    }
}
