package com.synchtask.notification.presentation.mapper

import com.synchtask.notification.application.dto.NotificationRedisDTO
import com.synchtask.notification.domain.entity.Notification
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NotificationMapperTest {

    private val user = User(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        passwordHash = "secure123"
    )

    private val now = LocalDateTime.now()

    private fun buildNotification(): Notification {
        return Notification(
            id = 42L,
            recipient = user,
            message = "Test notification",
            isRead = false,
            createdAt = now,
            type = NotificationType.TASK_UPDATE,
            groupId = 99L
        )
    }

    @Test
    fun `should map Notification to NotificationResponseDTO`() {
        val notification = buildNotification()

        val dto = NotificationMapper.toResponseDTO(notification)

        Assertions.assertEquals(42L, dto.id)
        Assertions.assertEquals("test@example.com", dto.recipientEmail)
        Assertions.assertEquals("Test notification", dto.message)
        Assertions.assertFalse(dto.isRead)
        Assertions.assertEquals(now, dto.createdAt)
        Assertions.assertEquals(NotificationType.TASK_UPDATE, dto.type)
        Assertions.assertEquals(99L, dto.groupId)
    }

    @Test
    fun `should map Notification to NotificationDTO`() {
        val notification = buildNotification()

        val dto = NotificationMapper.toWebSocketDTO(notification)

        Assertions.assertEquals(42L, dto.id)
        Assertions.assertEquals("test@example.com", dto.recipientEmail)
        Assertions.assertEquals("Test notification", dto.message)
        Assertions.assertEquals(now, dto.timestamp)
        Assertions.assertEquals(NotificationType.TASK_UPDATE, dto.type)
    }

    @Test
    fun `should map Notification to NotificationRedisDTO`() {
        val notification = buildNotification()

        val dto = NotificationMapper.toRedisDTO(notification)

        Assertions.assertEquals(42L, dto.id)
        Assertions.assertEquals("test@example.com", dto.recipientEmail)
        Assertions.assertEquals("Test notification", dto.message)
        Assertions.assertEquals(now, dto.createdAt)
        Assertions.assertEquals(NotificationType.TASK_UPDATE, dto.type)
    }

    @Test
    fun `should convert NotificationRedisDTO to NotificationResponseDTO`() {
        val redisDto = NotificationRedisDTO(
            id = 42L,
            recipientEmail = "test@example.com",
            message = "Test notification",
            createdAt = now,
            type = NotificationType.TASK_UPDATE
        )

        val responseDto = NotificationMapper.fromRedisDTO(redisDto)

        Assertions.assertEquals(42L, responseDto.id)
        Assertions.assertEquals("test@example.com", responseDto.recipientEmail)
        Assertions.assertEquals("Test notification", responseDto.message)
        Assertions.assertEquals(now, responseDto.createdAt)
        Assertions.assertEquals(NotificationType.TASK_UPDATE, responseDto.type)
        Assertions.assertFalse(responseDto.isRead)
        Assertions.assertNull(responseDto.groupId)
    }

    @Test
    fun `should throw when notification id is null`() {
        val notification = buildNotification().copy(id = null)

        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            NotificationMapper.toResponseDTO(notification)
        }

        Assertions.assertEquals("Notification ID cannot be null", exception.message)
    }
}
