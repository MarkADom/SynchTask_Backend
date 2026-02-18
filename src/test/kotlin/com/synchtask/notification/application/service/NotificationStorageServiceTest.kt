package com.synchtask.notification.application.service

import com.synchtask.notification.application.dto.NotificationRedisDTO
import com.synchtask.notification.domain.entity.Notification
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.domain.repository.NotificationRepository
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime
import java.time.Duration
import java.util.Optional

class NotificationStorageServiceTest {
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var redisTemplate: RedisTemplate<String, NotificationRedisDTO>
    private lateinit var hashOps: HashOperations<String, String, NotificationRedisDTO>
    private lateinit var service: NotificationStorageService

    private val user =
        User(
            id = 1L,
            name = "Test User",
            email = "user@example.com",
            passwordHash = "hash"
        )

    @BeforeEach
    fun setup() {
        notificationRepository = mockk()
        userRepository = mockk()
        redisTemplate = mockk()
        hashOps = mockk()

        every { redisTemplate.opsForHash<String, NotificationRedisDTO>() } returns hashOps
        every { redisTemplate.expire(any<String>(), any<Duration>()) } returns true

        service =
            NotificationStorageService(
                notificationRepository,
                userRepository,
                redisTemplate
            )
    }

    @Test
    fun `storeNotification should save to DB and cache in Redis`() {
        val notification =
            Notification(
                id = 42L,
                recipient = user,
                message = "Hello!",
                type = NotificationType.TASK_UPDATE
            )

        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { notificationRepository.save(any()) } returns notification
        every { hashOps.put(any(), any(), any()) } just runs

        val result =
            service.storeNotification(
                user.email,
                "Hello!",
                NotificationType.TASK_UPDATE
            )

        assertEquals(notification, result)
        verify(exactly = 1) {
            hashOps.put(
                "notifications:${user.email}",
                "42",
                any()
            )
        }
        verify(exactly = 1) { redisTemplate.expire("notifications:${user.email}", any<Duration>()) }
    }

    @Test
    fun `storeNotification should throw if user not found`() {
        every { userRepository.findByEmail(user.email) } returns Optional.empty()

        val ex =
            assertThrows(ResourceNotFoundException::class.java) {
                service.storeNotification(user.email, "Hi", NotificationType.TASK_UPDATE)
            }

        assertEquals("User not found: ${user.email}", ex.message)
    }

    @Test
    fun `getCachedNotifications should return list from Redis`() {
        val dto1 =
            NotificationRedisDTO(
                id = 1L,
                recipientEmail = user.email,
                message = "msg1",
                createdAt = LocalDateTime.now(),
                type = NotificationType.TASK_UPDATE
            )
        val dto2 =
            NotificationRedisDTO(
                id = 2L,
                recipientEmail = user.email,
                message = "msg2",
                createdAt = LocalDateTime.now(),
                type = NotificationType.TASK_UPDATE
            )

        every {
            hashOps.values("notifications:${user.email}")
        } returns listOf(dto1, dto2)

        val result = service.getCachedNotifications(user.email)

        assertEquals(2, result.size)
        assertTrue(result.containsAll(listOf(dto1, dto2)))
    }

    @Test
    fun `markAsRead should succeed if updated`() {
        every { notificationRepository.markAsReadById(42L) } returns 1

        assertDoesNotThrow {
            service.markAsRead(42L)
        }
    }

    @Test
    fun `markAsRead should throw if notification not found`() {
        every { notificationRepository.markAsReadById(42L) } returns 0

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                service.markAsRead(42L)
            }

        assertEquals("Notification with ID 42 not found.", ex.message)
    }
}
