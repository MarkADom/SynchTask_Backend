package com.synchtask.notification.application.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.presentation.mapper.NotificationMapper
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.project.domain.repository.ProjectMemberRepository
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.task.domain.repository.TaskRepository
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
    private val boardMemberRepository: BoardMemberRepository,
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val taskRepository: TaskRepository,
    private val taskMemberRepository: TaskMemberRepository,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional
    fun sendNotification(
        userEmail: String,
        message: String,
        type:
        NotificationType,
        groupId:
        Long? = null,
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

        requireNotificationContextPermission(actor, groupId, type)
        sendNotification(recipientEmail, message, type, groupId)
    }

    private fun requireNotificationContextPermission(
        actor: User,
        groupId: Long?,
        type: NotificationType,
    ) {
        if (actor.role == UserRole.ADMIN) return

        if (groupId == null) {
            throw UnauthorizedAccessException(
                "Notification dispatch requires context (groupId) for non-admin users"
            )
        }

        val actorId = actor.id ?: throw UnauthorizedAccessException("Actor ${actor.email} has invalid id")

        val hasBoardContextAccess =
            boardRepository.findById(groupId)
                .map { board ->
                    val boardId = board.id ?: return@map false
                    boardMemberRepository.existsByBoardIdAndUserId(boardId, actorId)
                }
                .orElse(false)

        val hasProjectContextAccess =
            projectRepository.findById(groupId)
                .map { project ->
                    val projectId = project.id ?: return@map false
                    projectMemberRepository.existsByProjectIdAndUserId(projectId, actorId)
                }
                .orElse(false)

        val hasTaskContextAccess =
            taskRepository.findById(groupId)
                .map { task ->
                    val taskId = task.id ?: return@map false
                    taskMemberRepository.existsByTaskIdAndUserId(taskId, actorId)
                }
                .orElse(false)

        if (!hasBoardContextAccess && !hasProjectContextAccess && !hasTaskContextAccess) {
            throw UnauthorizedAccessException(
                "Actor ${actor.email} has no contextual permission for groupId=$groupId and type=$type"
            )

        }
    }

    fun markAllAsRead(actorEmail: String, targetEmail: String = actorEmail) {
        val actor =
            userRepository.findByEmail(actorEmail)
                .orElseThrow { ResourceNotFoundException("User not found: $actorEmail") }

        if (actor.role != UserRole.ADMIN && actorEmail != targetEmail) {
            throw UnauthorizedAccessException("Non-admin users can only mark their own notifications as read")
        }

        notificationStorageService.markAllAsRead(targetEmail)
        logger.info("All notifications marked as read for $targetEmail (actor=$actorEmail)")
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
