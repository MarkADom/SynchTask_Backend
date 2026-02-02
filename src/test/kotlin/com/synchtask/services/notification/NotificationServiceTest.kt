package com.synchtask.services.notification

import com.synchtask.dtos.notification.NotificationRedisDTO
import com.synchtask.dtos.notification.NotificationResponseDTO
import com.synchtask.entities.Notification
import com.synchtask.entities.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.mappers.NotificationMapper
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NotificationServiceTest {

    private lateinit var storageService: NotificationStorageService
    private lateinit var webSocketService: NotificationWebSocketService
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        storageService = mockk()
        webSocketService = mockk()
        notificationService = NotificationService(storageService, webSocketService)
    }

    @Test
    fun `should send notification`() {
        val user = User(
            id = 1L,
            name = "Test User",
            email = "test@example.com",
            passwordHash = "123456"
        )

        val notification = Notification(
            id = 100L,
            recipient = user,
            message = "New Task",
            type = NotificationType.TASK_UPDATE
        )

        every {
            storageService.storeNotification(
                user.email,
                "New Task",
                NotificationType.TASK_UPDATE,
                null
            )
        } returns notification

        every { webSocketService.sendNotification(user.email, any()) } just runs
        every { storageService.updateDeliveryStatus(notification) } just runs

        notificationService.sendNotification(
            user.email,
            "New Task",
            NotificationType.TASK_UPDATE
        )

        verify(exactly = 1) { webSocketService.sendNotification(user.email, any()) }
        verify(exactly = 1) { storageService.updateDeliveryStatus(notification) }
    }

    @Test
    fun `should get unread notifications`() {
        val redisNotification = NotificationRedisDTO(
            id = 200L,
            recipientEmail = "user@example.com",
            message = "You have a new message",
            createdAt = LocalDateTime.now(),
            type = NotificationType.PERSONAL
        )

        every { storageService.getCachedNotifications("user@example.com") } returns listOf(redisNotification)

        val result: List<NotificationResponseDTO> =
            notificationService.getUnreadNotifications("user@example.com")

        assertEquals(1, result.size)
        assertEquals(NotificationMapper.fromRedisDTO(redisNotification), result.first())
    }

    @Test
    fun `markAsRead should not crash`() {
        every { storageService.markAsRead(42L) } just runs

        assertDoesNotThrow {
            notificationService.markAsRead(42L)
        }

        verify(exactly = 1) { storageService.markAsRead(42L) }
    }
}
