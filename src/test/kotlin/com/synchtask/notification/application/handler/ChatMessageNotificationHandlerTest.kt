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

class ChatMessageNotificationHandlerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var handler: ChatMessageNotificationHandler

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        handler = ChatMessageNotificationHandler(notificationService)

        user = User(
            id = 1L,
            name = "Marco",
            email = "marco@synchtask.com",
            passwordHash = "pw",
            role = UserRole.USER
        )
    }

    @Test
    fun `should support CHAT_MESSAGE notification type`() {
        assertTrue(handler.supports(NotificationType.CHAT_MESSAGE))
    }

    @Test
    fun `should not support other notification types`() {
        assertFalse(handler.supports(NotificationType.SYSTEM))
        assertFalse(handler.supports(NotificationType.TASK_UPDATE))
        assertFalse(handler.supports(NotificationType.PERSONAL))
    }

    @Test
    fun `should send chat message notification`() {
        handler.handle(user)

        verify(exactly = 1) {
            notificationService.sendNotification(
                user.email,
                "You received a new chat message.",
                NotificationType.CHAT_MESSAGE
            )
        }
    }
}
