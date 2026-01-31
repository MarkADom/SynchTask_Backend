package com.synchtask.services.notification

import com.fasterxml.jackson.core.JsonProcessingException
import com.synchtask.dtos.notification.NotificationResponseDTO
import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.mappers.NotificationMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Service

/**
 * **Notification Service**
 *
 * Orchestrates the creation, dispatch, and management of notifications.
 * Delegates persistence to NotificationStorageService and real-time delivery to NotificationWebSocketService.
 */
@Service
class NotificationService(
    private val notificationStorageService: NotificationStorageService,
    private val notificationWebSocketService: NotificationWebSocketService,
) {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Sends a new notification to the specified user.
     *
     * @param userEmail Recipient's email address.
     * @param message Notification message content.
     * @param type Type of the notification (SYSTEM, TASK_UPDATE, etc.).
     * @param groupId Optional group context.
     */
    @Transactional
    fun sendNotification(
        userEmail: String,
        message: String,
        type: NotificationType,
        groupId: Long? = null,
    ) {
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


    /**
     * Retrieves all unread notifications for the given user.
     */
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
        val onboardingMessages = listOf(
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

    /**
     * Clears all Redis entries for the given user.
     */
    fun clearRedisCacheForUser(userEmail: String): Int {
        val pattern = "notifications:$userEmail:*"
        val keys = notificationStorageService.getRedisKeys(pattern)
        if (keys.isEmpty()) return 0

        notificationStorageService.deleteRedisKeys(keys)
        logger.info("Cleared ${keys.size} cached notifications for $userEmail")
        return keys.size
    }
}
