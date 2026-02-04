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

class FriendRequestNotificationHandlerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var handler: FriendRequestNotificationHandler
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        handler = FriendRequestNotificationHandler(notificationService)

        user = User(
            id = 1L,
            name = "User",
            email = "user@synchtask.com",
            passwordHash = "pw",
            role = UserRole.USER
        )
    }

    @Test
    fun `should support FRIEND_REQUEST type`() {
        assertTrue(handler.supports(NotificationType.FRIEND_REQUEST))
    }

    @Test
    fun `should not support other types`() {
        assertFalse(handler.supports(NotificationType.SYSTEM))
    }

    @Test
    fun `should send friend request notification`() {
        handler.handle(user)

        verify(exactly = 1) {
            notificationService.sendNotification(
                user.email,
                "You have received a new friend request.",
                NotificationType.FRIEND_REQUEST
            )
        }
    }
}
