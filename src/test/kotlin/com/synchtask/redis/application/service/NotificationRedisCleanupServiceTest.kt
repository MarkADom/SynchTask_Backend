package com.synchtask.redis.application.service

import com.synchtask.notification.application.dto.NotificationRedisDTO
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime

class NotificationRedisCleanupServiceTest {
    private lateinit var redisTemplate: RedisTemplate<String, NotificationRedisDTO>
    private lateinit var hashOps: HashOperations<String, String, NotificationRedisDTO>
    private lateinit var service: NotificationRedisCleanupService

    @BeforeEach
    fun setup() {
        clearAllMocks()

        redisTemplate = mockk()
        hashOps = mockk()

        every { redisTemplate.opsForHash<String, NotificationRedisDTO>() } returns hashOps

        service = NotificationRedisCleanupService(redisTemplate)
    }

    @Test
    fun `should do nothing when no redis keys exist`() {
        every { redisTemplate.keys("notifications:*") } returns emptySet()

        service.cleanOldNotifications()

        verify(exactly = 1) { redisTemplate.keys("notifications:*") }
        verify(exactly = 0) { hashOps.entries(any()) }
        verify(exactly = 0) { hashOps.delete(any(), *anyVararg()) }
    }

    @Test
    fun `should not delete anything when notifications are not expired`() {
        val key = "notifications:user@test.com:1"

        val recentNotification =
            NotificationRedisDTO(
                id = 1L,
                recipientEmail = "user@test.com",
                message = "Recent",
                createdAt = LocalDateTime.now().minusHours(1),
                type = com.synchtask.notification.domain.entity.NotificationType.SYSTEM
            )

        every { redisTemplate.keys("notifications:*") } returns setOf(key)
        every { hashOps.entries(key) } returns mapOf("1" to recentNotification)

        service.cleanOldNotifications()

        verify(exactly = 1) { hashOps.entries(key) }
        verify(exactly = 0) { hashOps.delete(any(), *anyVararg()) }
    }

    @Test
    fun `should delete expired notifications`() {
        val key = "notifications:user@test.com:1"

        val expiredNotification =
            NotificationRedisDTO(
                id = 2L,
                recipientEmail = "user@test.com",
                message = "Old",
                createdAt = LocalDateTime.now().minusDays(2),
                type = com.synchtask.notification.domain.entity.NotificationType.SYSTEM
            )

        val validNotification =
            NotificationRedisDTO(
                id = 3L,
                recipientEmail = "user@test.com",
                message = "Still valid",
                createdAt = LocalDateTime.now().minusHours(2),
                type = com.synchtask.notification.domain.entity.NotificationType.SYSTEM
            )

        every { redisTemplate.keys("notifications:*") } returns setOf(key)
        every { hashOps.entries(key) } returns
            mapOf(
                "2" to expiredNotification,
                "3" to validNotification
            )

        every {
            hashOps.delete(key, "2")
        } returns 1L

        service.cleanOldNotifications()

        verify(exactly = 1) {
            hashOps.delete(key, "2")
        }
    }
}
