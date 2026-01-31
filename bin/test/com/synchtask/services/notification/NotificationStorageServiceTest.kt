package com.synchtask.services.notification

import com.synchtask.dtos.notification.NotificationRedisDTO
import com.synchtask.entities.Notification
import com.synchtask.entities.NotificationType
import com.synchtask.entities.User
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.repositories.NotificationRepository
import com.synchtask.repositories.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class NotificationStorageServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var redisTemplate: RedisTemplate<String, NotificationRedisDTO>
    private lateinit var valueOps: ValueOperations<String, NotificationRedisDTO>
    private lateinit var service: NotificationStorageService

    @BeforeEach
    fun setup() {
        notificationRepository = mockk()
        userRepository = mockk()
        redisTemplate = mockk()
        valueOps = mockk()

        every { redisTemplate.opsForValue() } returns valueOps

        service = NotificationStorageService(notificationRepository, userRepository, redisTemplate)
    }

    @Test
    fun `storeNotification should save to DB and cache in Redis`() {
        // Arrange
        val email = "user@example.com"
        val user = User(
            id = 1,
            name = "Test User",
            email = email,
            passwordHash = "securehash"
        )

        val notification = Notification(
            id = 42,
            recipient = user,
            message = "Hello!",
            type = NotificationType.TASK_UPDATE,
            groupId = null
        )

        every { userRepository.findByEmail(email) } returns Optional.of(user)
        every { notificationRepository.save(any()) } returns notification
        every { valueOps.set(any(), any(), any<Duration>()) } just Runs

        // Act
        val result = service.storeNotification(email, "Hello!", NotificationType.TASK_UPDATE)

        // Assert
        assertEquals(notification, result)
        verify { userRepository.findByEmail(email) }
        verify { notificationRepository.save(any()) }
        verify { valueOps.set(match { it.startsWith("notifications:") }, any(), any<Duration>()) }
    }

    @Test
    fun `storeNotification should throw if user not found`() {
        // Arrange
        val email = "unknown@example.com"
        every { userRepository.findByEmail(email) } returns Optional.empty()

        // Act & Assert
        val ex = assertThrows(ResourceNotFoundException::class.java) {
            service.storeNotification(email, "Hi", NotificationType.TASK_UPDATE)
        }

        assertEquals("User not found: $email", ex.message)
    }

    @Test
    fun `getCachedNotifications should return list from Redis`() {
        // Arrange
        val email = "user@example.com"
        val key1 = "notifications:$email:1"
        val key2 = "notifications:$email:2"
        val dto1 = NotificationRedisDTO(1, email, "msg1", LocalDateTime.now(), NotificationType.TASK_UPDATE)
        val dto2 = NotificationRedisDTO(2, email, "msg2", LocalDateTime.now(), NotificationType.TASK_UPDATE)

        every { redisTemplate.keys("notifications:$email:*") } returns setOf(key1, key2)
        every { redisTemplate.opsForValue() } returns valueOps
        every { valueOps.get(key1) } returns dto1
        every { valueOps.get(key2) } returns dto2

        // Act
        val result = service.getCachedNotifications(email)

        // Assert
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

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.markAsRead(42L)
        }

        assertEquals("Notification with ID 42 not found.", ex.message)
    }
}
