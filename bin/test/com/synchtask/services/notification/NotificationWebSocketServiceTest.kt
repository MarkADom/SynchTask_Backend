package com.synchtask.services.notification

import com.synchtask.dtos.notification.NotificationDTO
import com.synchtask.entities.NotificationType
import com.synchtask.managers.WebSocketManager
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate

class NotificationWebSocketServiceTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var storageService: NotificationStorageService
    private lateinit var notificationWebSocketService: NotificationWebSocketService

    @BeforeEach
    fun setup() {
        messagingTemplate = mockk(relaxed = true)
        webSocketManager = mockk()
        storageService = mockk(relaxed = true)

        notificationWebSocketService = NotificationWebSocketService(
            messagingTemplate,
            webSocketManager,
            storageService
        )
    }

    @Test
    fun `should send notification via WebSocket when user is online`() {
        // Arrange
        val email = "user@example.com"
        val dto = NotificationDTO(
            recipientEmail = email,
            message = "You have a task",
            type = NotificationType.TASK_UPDATE
        )

        every { webSocketManager.isUserOnline(email) } returns true

        // Act
        notificationWebSocketService.sendNotification(email, dto)

        // Assert
        verify(exactly = 1) {
            messagingTemplate.convertAndSendToUser(email, "/queue/notifications", dto)
        }
        verify(exactly = 0) {
            storageService.storeNotification(any(), any(), any())
        }
    }

    @Test
    fun `should store notification when user is offline`() {
        // Arrange
        val email = "offline@example.com"
        val dto = NotificationDTO(
            recipientEmail = email,
            message = "New message",
            type = NotificationType.TASK_UPDATE
        )

        every { webSocketManager.isUserOnline(email) } returns false

        // Act
        notificationWebSocketService.sendNotification(email, dto)

        // Assert
        verify(exactly = 0) {
            messagingTemplate.convertAndSendToUser(any(), any(), any())
        }
        verify(exactly = 1) {
            storageService.storeNotification(email, dto.message, dto.type)
        }
    }
}
