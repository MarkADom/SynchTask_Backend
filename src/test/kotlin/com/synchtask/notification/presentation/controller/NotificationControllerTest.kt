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

        every {
            notificationService.sendNotification(
                request.email,
                request.message,
                request.type,
                request.groupId
            )
        } just Runs

        val response = controller.sendNotification(request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Notification sent successfully", response.body?.message)
        verify(exactly = 1) {
            notificationService.sendNotification(
                request.email,
                request.message,
                request.type,
                request.groupId
            )
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
}
