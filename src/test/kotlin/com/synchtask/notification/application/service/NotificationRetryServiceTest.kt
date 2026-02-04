package com.synchtask.notification.application.service

import com.synchtask.notification.domain.entity.Notification
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.domain.repository.NotificationRepository
import com.synchtask.user.domain.entity.User
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NotificationRetryServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var webSocketService: NotificationWebSocketService
    private lateinit var service: NotificationRetryService

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        clearAllMocks()

        notificationRepository = mockk()
        webSocketService = mockk()

        service = NotificationRetryService(
            notificationRepository,
            webSocketService
        )

        user = User(
            id = 1L,
            name = "Test",
            email = "test@example.com",
            passwordHash = "pw"
        )
    }

    @Test
    fun `should do nothing when no undelivered notifications exist`() {
        every { notificationRepository.findAllByDeliveredFalse() } returns emptyList()

        service.retryUndeliveredNotifications()

        verify(exactly = 1) {
            notificationRepository.findAllByDeliveredFalse()
        }

        verify(exactly = 0) {
            webSocketService.sendNotification(any(), any())
        }
    }

    @Test
    fun `should retry and mark notification as delivered`() {
        val notification = Notification(
            id = 10L,
            recipient = user,
            message = "Test message",
            type = NotificationType.SYSTEM,
            delivered = false
        )

        every {
            notificationRepository.findAllByDeliveredFalse()
        } returns listOf(notification)

        every {
            webSocketService.sendNotification(user.email, any())
        } just Runs

        every {
            notificationRepository.save(notification)
        } returns notification

        service.retryUndeliveredNotifications()

        assertEquals(true, notification.delivered)

        verify(exactly = 1) {
            webSocketService.sendNotification(user.email, any())
        }

        verify(exactly = 1) {
            notificationRepository.save(notification)
        }
    }

    @Test
    fun `should not crash when resend fails`() {
        val notification = Notification(
            id = 99L,
            recipient = user,
            message = "Failing message",
            type = NotificationType.SYSTEM,
            delivered = false
        )

        every {
            notificationRepository.findAllByDeliveredFalse()
        } returns listOf(notification)

        every {
            webSocketService.sendNotification(user.email, any())
        } throws RuntimeException("WebSocket down")

        service.retryUndeliveredNotifications()

        // delivered must stay false
        assertEquals(false, notification.delivered)

        verify(exactly = 1) {
            webSocketService.sendNotification(user.email, any())
        }

        verify(exactly = 0) {
            notificationRepository.save(any())
        }
    }
}
