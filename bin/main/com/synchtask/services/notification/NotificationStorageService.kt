package com.synchtask.services.notification

import com.synchtask.dtos.notification.NotificationRedisDTO
import com.synchtask.entities.Notification
import com.synchtask.entities.NotificationType
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.NotificationRepository
import com.synchtask.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * **Notification Storage Service**
 *
 * Handles **storing** notifications in the database and caching in Redis using Redis Hash.
 */
@Service
class NotificationStorageService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val redisTemplate: RedisTemplate<String, NotificationRedisDTO>,
) {

    private val logger = LoggerFactory.getLogger(NotificationStorageService::class.java)

    companion object {
        private val NOTIFICATION_CACHE_TTL: Duration = Duration.ofHours(24)
    }

    /**
     * Stores a notification in the database and Redis Hash cache.
     */
    fun storeNotification(
        recipientEmail: String,
        message: String,
        type: NotificationType,
        groupId: Long? = null,
    ): Notification {
        val user = userRepository.findByEmail(recipientEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $recipientEmail") }

        val notification = Notification(
            recipient = user,
            message = message,
            type = type,
            groupId = groupId
        )

        val savedNotification = notificationRepository.save(notification)
        val redisKey = "notifications:$recipientEmail"
        val redisField = savedNotification.id!!.toString()
        val notificationDTO = NotificationRedisDTO.fromEntity(savedNotification)

        redisTemplate.opsForHash<String, NotificationRedisDTO>().put(redisKey, redisField, notificationDTO)

        logger.info("Notification stored in DB and cached in Redis hash: $message for $recipientEmail")
        return savedNotification
    }

    /**
     * Fetches all cached notifications for the user from Redis Hash.
     */
    fun getCachedNotifications(userEmail: String): List<NotificationRedisDTO> {
        val redisKey = "notifications:$userEmail"
        val values = redisTemplate.opsForHash<String, NotificationRedisDTO>().values(redisKey)
        return values.toList()
    }

    /**
     * Deletes all cached notifications for a user from Redis Hash.
     */
    fun deleteRedisHash(userEmail: String) {
        val redisKey = "notifications:$userEmail"
        redisTemplate.delete(redisKey)
    }

    /**
     * Legacy method: still supports direct key deletion if needed.
     */
    fun getRedisKeys(pattern: String): Set<String> {
        return redisTemplate.keys(pattern)
    }

    fun deleteRedisKeys(keys: Collection<String>) {
        redisTemplate.delete(keys)
    }

    @Transactional
    fun markAsRead(notificationId: Long) {
        val updatedCount = notificationRepository.markAsReadById(notificationId)
        if (updatedCount == 0) {
            throw IllegalArgumentException("Notification with ID $notificationId not found.")
        }
    }

    @Transactional
    fun markAllAsRead(userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val updated = notificationRepository.markAllAsReadByRecipient(user)
        logger.info("Marked $updated notifications as read for user: $userEmail")
    }

    @Transactional
    fun updateDeliveryStatus(notification: Notification) {
        notificationRepository.save(notification)
    }
}
