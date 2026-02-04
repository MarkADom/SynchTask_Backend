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

class InvitationNotificationHandlerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var handler: InvitationNotificationHandler
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        handler = InvitationNotificationHandler(notificationService)

        user = User(
            id = 1L,
            name = "User",
            email = "user@synchtask.com",
            passwordHash = "pw",
            role = UserRole.USER
        )
    }

    @Test
    fun `should support INVITATION type`() {
        assertTrue(handler.supports(NotificationType.INVITATION))
    }

    @Test
    fun `should not support other types`() {
        assertFalse(handler.supports(NotificationType.SYSTEM))
    }

    @Test
    fun `should send invitation notification`() {
        handler.handle(user)

        verify(exactly = 1) {
            notificationService.sendNotification(
                user.email,
                "You've been invited to collaborate on a board!",
                NotificationType.INVITATION
            )
        }
    }
}
