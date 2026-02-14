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

class TaskUpdateNotificationHandlerTest {
    private lateinit var notificationService: NotificationService
    private lateinit var handler: TaskUpdateNotificationHandler
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        handler = TaskUpdateNotificationHandler(notificationService)

        user =
            User(
                id = 1L,
                name = "User",
                email = "user@synchtask.com",
                passwordHash = "pw",
                role = UserRole.USER
            )
    }

    @Test
    fun `should support TASK_UPDATE type`() {
        assertTrue(handler.supports(NotificationType.TASK_UPDATE))
    }

    @Test
    fun `should not support other types`() {
        assertFalse(handler.supports(NotificationType.SYSTEM))
    }

    @Test
    fun `should send task update notification`() {
        handler.handle(user)

        verify(exactly = 1) {
            notificationService.sendNotification(
                user.email,
                "A task assigned to you has been updated.",
                NotificationType.TASK_UPDATE
            )
        }
    }
}
