package com.synchtask.mappers

import com.synchtask.dtos.notification.NotificationRedisDTO
import com.synchtask.entities.Notification
import com.synchtask.entities.NotificationType
import com.synchtask.user.domain.entity.User
import org.junit.jupiter.api.Assertions.*
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

        assertEquals(42L, dto.id)
        assertEquals("test@example.com", dto.recipientEmail)
        assertEquals("Test notification", dto.message)
        assertFalse(dto.isRead)
        assertEquals(now, dto.createdAt)
        assertEquals(NotificationType.TASK_UPDATE, dto.type)
        assertEquals(99L, dto.groupId)
    }

    @Test
    fun `should map Notification to NotificationDTO`() {
        val notification = buildNotification()

        val dto = NotificationMapper.toWebSocketDTO(notification)

        assertEquals(42L, dto.id)
        assertEquals("test@example.com", dto.recipientEmail)
        assertEquals("Test notification", dto.message)
        assertEquals(now, dto.timestamp)
        assertEquals(NotificationType.TASK_UPDATE, dto.type)
    }

    @Test
    fun `should map Notification to NotificationRedisDTO`() {
        val notification = buildNotification()

        val dto = NotificationMapper.toRedisDTO(notification)

        assertEquals(42L, dto.id)
        assertEquals("test@example.com", dto.recipientEmail)
        assertEquals("Test notification", dto.message)
        assertEquals(now, dto.createdAt)
        assertEquals(NotificationType.TASK_UPDATE, dto.type)
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

        assertEquals(42L, responseDto.id)
        assertEquals("test@example.com", responseDto.recipientEmail)
        assertEquals("Test notification", responseDto.message)
        assertEquals(now, responseDto.createdAt)
        assertEquals(NotificationType.TASK_UPDATE, responseDto.type)
        assertFalse(responseDto.isRead)
        assertNull(responseDto.groupId)
    }

    @Test
    fun `should throw when notification id is null`() {
        val notification = buildNotification().copy(id = null)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            NotificationMapper.toResponseDTO(notification)
        }

        assertEquals("Notification ID cannot be null", exception.message)
    }
}
