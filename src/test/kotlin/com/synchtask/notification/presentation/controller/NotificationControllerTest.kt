package com.synchtask.notification.presentation.controller

import com.synchtask.notification.application.dto.NotificationRequestDTO
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.domain.entity.NotificationType
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NotificationControllerTest {
    private lateinit var notificationService: NotificationService
    private lateinit var controller: NotificationController

    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        controller = NotificationController(notificationService)
    }

    @Test
    fun `should send notification successfully`() {
        val request =
            NotificationRequestDTO(
                email = "user@example.com",
                message = "Hello!",
                type = NotificationType.PERSONAL,
                groupId = 1L
            )

        val principal = mockk<UserDetails> { every { username } returns "owner@example.com" }

        every {
            notificationService.sendNotificationAsActor(any(), any(), any(), any(), any())
        } just Runs

        val response = controller.sendNotification(principal, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Notification sent successfully", response.body?.message)
        verify(exactly = 1) {
            notificationService.sendNotificationAsActor(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `should return unread notifications for authenticated user`() {
        val userEmail = "user@example.com"
        val principal = mockk<UserDetails> { every { username } returns userEmail }
        val now = LocalDateTime.now()

        val expectedNotifications =
            listOf(
                NotificationResponseDTO(
                    1L,
                    userEmail,
                    "You have a new task",
                    false,
                    now,
                    NotificationType.GROUP,
                    42L
                ),
                NotificationResponseDTO(
                    2L,
                    userEmail,
                    "System update completed",
                    false,
                    now,
                    NotificationType.SYSTEM,
                    null
                )
            )

        every { notificationService.getUnreadNotifications(userEmail) } returns expectedNotifications

        val response = controller.getMyUnreadNotifications(principal)


        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expectedNotifications, response.body)

        verify(exactly = 1) { notificationService.getUnreadNotifications(userEmail) }
    }

    @Test
    fun `should mark one notification as read`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        every { notificationService.markAsRead(10L, "me@example.com") } just Runs

        val response = controller.markNotificationAsRead(10L, principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Notification marked as read", response.body?.message)
        verify(exactly = 1) { notificationService.markAsRead(10L, "me@example.com") }
    }

    @Test
    fun `should mark all my notifications as read`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        every { notificationService.markAllAsRead("me@example.com") } just Runs

        val response = controller.markAllMyNotificationsAsRead(principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("All notifications marked as read", response.body?.message)
        verify(exactly = 1) { notificationService.markAllAsRead("me@example.com") }
    }

    @Test
    fun `should mark all notifications as read on legacy endpoint`() {
        val principal = mockk<UserDetails> { every { username } returns "legacy@example.com" }
        every { notificationService.markAllAsRead("legacy@example.com") } just Runs

        val response = controller.markAllNotificationsAsRead("legacy@example.com", principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("All notifications marked as read", response.body?.message)
        verify(exactly = 1) { notificationService.markAllAsRead("legacy@example.com") }
    }

    @Test
    fun `should clear my notification cache`() {
        val principal = mockk<UserDetails> { every { username } returns "me@example.com" }
        every { notificationService.clearRedisCacheForUser("me@example.com") } returns 3

        val response = controller.clearMyNotificationCache(principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Cleared 3 notification(s) from Redis cache", response.body?.message)
        verify(exactly = 1) { notificationService.clearRedisCacheForUser("me@example.com") }
    }

    @Test
    fun `should clear notification cache on legacy endpoint`() {
        val principal = mockk<UserDetails> { every { username } returns "legacy@example.com" }
        every { notificationService.clearRedisCacheForUser("legacy@example.com") } returns 1

        val response = controller.clearNotificationCache("legacy@example.com", principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Cleared 1 notification(s) from Redis cache", response.body?.message)
        verify(exactly = 1) { notificationService.clearRedisCacheForUser("legacy@example.com") }
    }
}
