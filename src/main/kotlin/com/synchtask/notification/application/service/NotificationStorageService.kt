package com.synchtask.notification.application.service

import com.synchtask.notification.application.dto.NotificationRedisDTO
import com.synchtask.notification.domain.entity.Notification
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.domain.repository.NotificationRepository
import com.synchtask.notification.presentation.mapper.NotificationMapper
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

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

    fun storeNotification(
        recipientEmail: String,
        message: String,
        type: NotificationType,
        groupId: Long? = null,
    ): Notification {
        val user =
            userRepository.findByEmail(recipientEmail)
                .orElseThrow { ResourceNotFoundException("User not found: $recipientEmail") }

        val notification =
            Notification(
                recipient = user,
                message = message,
                type = type,
                groupId = groupId
            )

        val savedNotification = notificationRepository.save(notification)
        val redisKey = "notifications:$recipientEmail"
        val redisField = checkNotNull(savedNotification.id) { "Notification ID cannot be null" }.toString()
        val notificationDTO = NotificationMapper.toRedisDTO(savedNotification)

        redisTemplate.opsForHash<String, NotificationRedisDTO>().put(redisKey, redisField, notificationDTO)
        redisTemplate.expire(redisKey, NOTIFICATION_CACHE_TTL)

        logger.info("Notification stored in DB and cached in Redis hash: $message for $recipientEmail")
        return savedNotification
    }

    fun getCachedNotifications(userEmail: String): List<NotificationRedisDTO> {
        val redisKey = "notifications:$userEmail"
        val values = redisTemplate.opsForHash<String, NotificationRedisDTO>().values(redisKey)
        return values.toList()
    }

    fun deleteRedisHash(userEmail: String) {
        val redisKey = "notifications:$userEmail"
        redisTemplate.delete(redisKey)
    }

    fun getRedisKeys(pattern: String): Set<String> {
        return redisTemplate.keys(pattern) ?: emptySet()
    }

    fun deleteRedisKeys(keys: Collection<String>) {
        redisTemplate.delete(keys)
    }

    @Transactional
    fun markAsRead(notificationId: Long) {
        val updatedCount = notificationRepository.markAsReadById(notificationId)
        require(updatedCount > 0) { "Notification with ID $notificationId not found." }
    }

    @Transactional
    fun markAsRead(notificationId: Long, userEmail: String) {
        val updatedCount = notificationRepository.markAsReadByIdAndRecipientEmail(notificationId, userEmail)
        if (updatedCount <= 0) {
            throw com.synchtask.shared.exception.UnauthorizedAccessException(
                "Notification with ID $notificationId was not found for the authenticated user"
            )
        }
    }

    @Transactional
    fun markAllAsRead(userEmail: String) {
        val user =
            userRepository.findByEmail(userEmail)
                .orElseThrow { ResourceNotFoundException("User not found: $userEmail") }

        val updated = notificationRepository.markAllAsReadByRecipient(user)
        logger.info("Marked $updated notifications as read for user: $userEmail")
    }

    @Transactional
    fun updateDeliveryStatus(notification: Notification) {
        notificationRepository.save(notification)
    }
}
