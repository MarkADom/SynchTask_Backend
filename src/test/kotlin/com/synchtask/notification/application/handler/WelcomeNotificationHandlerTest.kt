package com.synchtask.notification.application.handler

import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WelcomeNotificationHandlerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var handler: WelcomeNotificationHandler

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        handler = WelcomeNotificationHandler(notificationService)

        user = User(
            id = 1L,
            name = "Marco",
            email = "marco@synchtask.com",
            passwordHash = "pw",
            role = UserRole.USER
        )
    }

    @Test
    fun `should send welcome notifications`() {
        handler.handle(user)

        verify(exactly = 3) {
            notificationService.sendNotification(
                user.email,
                any(),
                NotificationType.SYSTEM
            )
        }

        verify {
            notificationService.sendNotification(
                user.email,
                "Welcome to SynchTask! We're excited to have you onboard.",
                NotificationType.SYSTEM
            )
            notificationService.sendNotification(
                user.email,
                "You can start by creating your first task or inviting friends.",
                NotificationType.SYSTEM
            )
            notificationService.sendNotification(
                user.email,
                "Check your dashboard for updates, tips and team activity.",
                NotificationType.SYSTEM
            )
        }
    }

    @Test
    fun `should support SYSTEM notification type`() {
        assertTrue(handler.supports(NotificationType.SYSTEM))
    }

    @Test
    fun `should not support non SYSTEM notification types`() {
        assertFalse(handler.supports(NotificationType.TASK_UPDATE))
        assertFalse(handler.supports(NotificationType.PERSONAL))
    }
}
