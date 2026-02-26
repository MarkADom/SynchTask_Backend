package com.synchtask.notification.application.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.presentation.mapper.NotificationMapper
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val notificationStorageService: NotificationStorageService,
    private val notificationWebSocketService: NotificationWebSocketService,
    private val userRepository: UserRepository,
    private val boardRepository: BoardRepository,
    private val projectRepository: ProjectRepository,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional
    fun sendNotification(userEmail: String, message: String, type: NotificationType, groupId: Long? = null) {
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

    fun markAsRead(notificationId: Long, actorEmail: String) {
        val actor =
            userRepository.findByEmail(actorEmail)
                .orElseThrow { ResourceNotFoundException("User not found: $actorEmail") }

        if (actor.role == UserRole.ADMIN) {
            notificationStorageService.markAsRead(notificationId)
            return
        }

        notificationStorageService.markAsRead(notificationId, actorEmail)
    }

    fun sendNotificationAsActor(
        actorEmail: String,
        recipientEmail: String,
        message: String,
        type: NotificationType,
        groupId: Long? = null,
    ) {
        val actor =
            userRepository.findByEmail(actorEmail)
                .orElseThrow { ResourceNotFoundException("User not found: $actorEmail") }

        requireNotificationContextPermission(actor, groupId)
        sendNotification(recipientEmail, message, type, groupId)
    }

    private fun requireNotificationContextPermission(actor: User, groupId: Long?) {
        if (actor.role == UserRole.ADMIN) return

        if (groupId == null) {
            throw UnauthorizedAccessException("Notification dispatch requires context (groupId) for non-admin users")
        }

        val hasBoardContextAccess =
            boardRepository.findById(groupId)
                .map { board ->
                    board.owner.email == actor.email ||
                        board.collaborators.any { collaborator -> collaborator.email == actor.email }
                }
                .orElse(false)

        val hasProjectContextAccess =
            projectRepository.findById(groupId)
                .map { project ->
                    project.owner.email == actor.email ||
                        project.members.any { member -> member.email == actor.email }
                }
                .orElse(false)

        if (!hasBoardContextAccess && !hasProjectContextAccess) {
            throw UnauthorizedAccessException("Actor ${actor.email} has no contextual permission for groupId=$groupId")
        }
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
