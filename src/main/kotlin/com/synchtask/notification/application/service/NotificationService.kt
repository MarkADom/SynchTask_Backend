package com.synchtask.notification.application.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.presentation.mapper.NotificationMapper
import com.synchtask.user.domain.entity.User
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val notificationStorageService: NotificationStorageService,
    private val notificationWebSocketService: NotificationWebSocketService,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional
    fun sendNotification(userEmail: String, message: String, type: NotificationType, groupId: Long? = null,) {
        val notification = notificationStorageService.storeNotification(userEmail, message, type, groupId)
        try {
            val dto = NotificationMapper.toWebSocketDTO(notification)
            notificationWebSocketService.sendNotification(userEmail, dto)

            notification.delivered = true
            notificationStorageService.updateDeliveryStatus(notification)

            logger.info("Notification sent successfully to $userEmail: $message")
        } catch (ex: MessagingException) {
            logger.error("WebSocket delivery failed for $userEmail. Notification persisted. Reason: ${ex.message}", ex)
        } catch (ex: JsonProcessingException) {
            logger.error("Serialization error for notification DTO to $userEmail: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid notification data for $userEmail: ${ex.message}", ex)
        }
    }

    fun getUnreadNotifications(userEmail: String): List<NotificationResponseDTO> {
        val cachedNotifications = notificationStorageService.getCachedNotifications(userEmail)
        return cachedNotifications.map { NotificationMapper.fromRedisDTO(it) }
    }

    fun markAsRead(notificationId: Long) {
        notificationStorageService.markAsRead(notificationId)
    }

    fun markAllAsRead(userEmail: String) {
        notificationStorageService.markAllAsRead(userEmail)
        logger.info("All notifications marked as read for $userEmail")
    }

    fun sendFirstLoginNotifications(user: User) {
        val onboardingMessages =
            listOf(
                "Welcome to SynchTask, ${user.name}!",
                "Here's a tip: you can create boards and invite collaborators.",
                "Try adding your first task now!",
                "Check out our roadmap to see what’s coming next!"
            )

        onboardingMessages.forEach { message ->
            sendNotification(user.email, message, NotificationType.SYSTEM)
        }

        logger.info("Onboarding notifications sent to ${user.email}")
    }

    fun clearRedisCacheForUser(userEmail: String): Int {
        val pattern = "notifications:$userEmail:*"
        val keys = notificationStorageService.getRedisKeys(pattern)
        if (keys.isEmpty()) return 0

        notificationStorageService.deleteRedisKeys(keys)
        logger.info("Cleared ${keys.size} cached notifications for $userEmail")
        return keys.size
    }
}
