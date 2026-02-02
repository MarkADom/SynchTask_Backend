package com.synchtask.controllers

import com.synchtask.notification.application.dto.NotificationRequestDTO
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.notification.presentation.controller.NotificationController
import com.synchtask.services.redis.NotificationRedisCleanupService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NotificationControllerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var redisCleanupService: NotificationRedisCleanupService;
    private lateinit var controller: NotificationController


    @BeforeEach
    fun setup() {
        notificationService = mockk(relaxed = true)
        redisCleanupService = mockk(relaxed = true)
        controller = NotificationController(notificationService, redisCleanupService)
    }

    @Test
    fun `should send notification successfully`() {
        // Arrange
        val request = NotificationRequestDTO(
            email = "user@example.com",
            message = "Hello!",
            type = NotificationType.PERSONAL,
            groupId = 1L
        )

        every {
            notificationService.sendNotification(
                request.email, request.message, request.type, request.groupId
            )
        } just Runs

        // Act
        val response = controller.sendNotification(request)

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Notification sent successfully", response.body)

        verify(exactly = 1) {
            notificationService.sendNotification(
                request.email, request.message, request.type, request.groupId
            )
        }
    }

    @Test
    fun `should return list of unread notifications`() {
        // Arrange
        val userEmail = "user@example.com"
        val now = LocalDateTime.now()

        val expectedNotifications = listOf(
            NotificationResponseDTO(
                id = 1L,
                recipientEmail = userEmail,
                message = "You have a new task",
                isRead = false,
                createdAt = now,
                type = NotificationType.GROUP,
                groupId = 42L
            ),
            NotificationResponseDTO(
                id = 2L,
                recipientEmail = userEmail,
                message = "System update completed",
                isRead = false,
                createdAt = now,
                type = NotificationType.SYSTEM,
                groupId = null
            )
        )

        every { notificationService.getUnreadNotifications(userEmail) } returns expectedNotifications

        // Act
        val response = controller.getUnreadNotifications(userEmail)

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expectedNotifications, response.body)

        verify(exactly = 1) {
            notificationService.getUnreadNotifications(userEmail)
        }
    }
}
