package com.synchtask.notification.application.service

import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.notification.application.dto.NotificationRedisDTO
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.domain.entity.Notification
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.presentation.mapper.NotificationMapper
import com.synchtask.project.domain.entity.Project
import com.synchtask.project.domain.repository.ProjectRepository
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.entity.User
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationServiceTest {
    private lateinit var storageService: NotificationStorageService
    private lateinit var webSocketService: NotificationWebSocketService
    private lateinit var userRepository: UserRepository
    private lateinit var boardRepository: BoardRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        storageService = mockk()
        webSocketService = mockk()
        userRepository = mockk()
        boardRepository = mockk()
        projectRepository = mockk()
        taskRepository = mockk()
        notificationService = NotificationService(
            storageService,
            webSocketService,
            userRepository,
            boardRepository,
            projectRepository,
            taskRepository,
        )
    }

    @Test
    fun `should send notification`() {
        val user =
            User(
                id = 1L,
                name = "Test User",
                email = "test@example.com",
                passwordHash = "123456"
            )

        val notification =
            Notification(
                id = 100L,
                recipient = user,
                message = "New Task",
                type = NotificationType.TASK_UPDATE
            )

        every {
            storageService.storeNotification(
                user.email,
                "New Task",
                NotificationType.TASK_UPDATE,
                null
            )
        } returns notification

        every { webSocketService.sendNotification(user.email, any()) } just runs
        every { storageService.updateDeliveryStatus(notification) } just runs

        notificationService.sendNotification(
            user.email,
            "New Task",
            NotificationType.TASK_UPDATE
        )

        verify(exactly = 1) { webSocketService.sendNotification(user.email, any()) }
        verify(exactly = 1) { storageService.updateDeliveryStatus(notification) }
    }

    @Test
    fun `should mark all notifications as read for actor scope`() {
        val actor =
            User(
                id = 20L,
                name = "User",
                email = "user@example.com",
                passwordHash = "pw",
                role = UserRole.USER
            )
        every { userRepository.findByEmail("user@example.com") } returns java.util.Optional.of(actor)
        every { storageService.markAllAsRead("user@example.com") } just Runs

        notificationService.markAllAsRead("user@example.com")

        verify(exactly = 1) { storageService.markAllAsRead("user@example.com") }
    }


    @Test
    fun `should send onboarding notifications`() {
        val user =
            User(
                id = 1L,
                name = "Test",
                email = "test@example.com",
                passwordHash = "pw"
            )

        every {
            storageService.storeNotification(any(), any(), any(), any())
        } returns
            Notification(
                id = 1L,
                recipient = user,
                message = "msg",
                type = NotificationType.SYSTEM
            )

        every { webSocketService.sendNotification(any(), any()) } just Runs
        every { storageService.updateDeliveryStatus(any()) } just Runs

        notificationService.sendFirstLoginNotifications(user)

        verify(exactly = 4) {
            storageService.storeNotification(
                user.email,
                any(),
                NotificationType.SYSTEM,
                null
            )
        }
    }

    @Test
    fun `should get unread notifications`() {
        val redisNotification =
            NotificationRedisDTO(
                id = 200L,
                recipientEmail = "user@example.com",
                message = "You have a new message",
                createdAt = LocalDateTime.now(),
                type = NotificationType.PERSONAL
            )

        every { storageService.getCachedNotifications("user@example.com") } returns listOf(redisNotification)

        val result: List<NotificationResponseDTO> =
            notificationService.getUnreadNotifications("user@example.com")

        assertEquals(1, result.size)
        assertEquals(NotificationMapper.fromRedisDTO(redisNotification), result.first())
    }

    @Test
    fun `markAsRead should not crash for admin`() {
        val admin =
            User(
                id = 99L,
                name = "Admin",
                email = "admin@example.com",
                passwordHash = "pw",
                role = UserRole.ADMIN
            )

        every { userRepository.findByEmail("admin@example.com") } returns java.util.Optional.of(admin)
        every { storageService.markAsRead(42L) } just runs

        assertDoesNotThrow {
            notificationService.markAsRead(42L, "admin@example.com")
        }

        verify(exactly = 1) { storageService.markAsRead(42L) }
    }

    @Test
    fun `should clear redis cache when keys exist`() {
        val keys = setOf("k1", "k2", "k3")

        every {
            storageService.getRedisKeys("notifications:user@example.com:*")
        } returns keys

        every { storageService.deleteRedisKeys(keys) } just Runs

        val deleted = notificationService.clearRedisCacheForUser("user@example.com")

        assertEquals(3, deleted)

        verify { storageService.deleteRedisKeys(keys) }
    }

    @Test
    fun `should return zero when no redis keys exist`() {
        every {
            storageService.getRedisKeys("notifications:user@example.com:*")
        } returns emptySet()

        val deleted = notificationService.clearRedisCacheForUser("user@example.com")

        assertEquals(0, deleted)

        verify(exactly = 0) {
            storageService.deleteRedisKeys(any())
        }
    }

    @Test
    fun `markAsRead should use recipient scoped update for non admin`() {
        val actor =
            User(
                id = 2L,
                name = "User",
                email = "user@example.com",
                passwordHash = "pw",
                role = UserRole.USER
            )

        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)
        every { storageService.markAsRead(42L, actor.email) } just runs

        notificationService.markAsRead(42L, actor.email)

        verify(exactly = 1) { storageService.markAsRead(42L, actor.email) }
        verify(exactly = 0) { storageService.markAsRead(42L) }
    }

    @Test
    fun `markAsRead should throw when actor is unknown`() {
        every { userRepository.findByEmail("missing@example.com") } returns java.util.Optional.empty()

        val ex = assertFailsWith<ResourceNotFoundException> {
            notificationService.markAsRead(10L, "missing@example.com")
        }

        assertEquals("User not found: missing@example.com", ex.message)
    }

    @Test
    fun `sendNotificationAsActor should allow admin without context`() {
        val admin =
            User(
                id = 5L,
                name = "Admin",
                email = "admin@example.com",
                passwordHash = "pw",
                role = UserRole.ADMIN
            )
        val recipient =
            User(
                id = 6L,
                name = "Recipient",
                email = "recipient@example.com",
                passwordHash = "pw"
            )
        val notification =
            Notification(
                id = 300L,
                recipient = recipient,
                message = "hello",
                type = NotificationType.SYSTEM
            )

        every { userRepository.findByEmail(admin.email) } returns java.util.Optional.of(admin)
        every {
            storageService.storeNotification(
                recipient.email,
                "hello",
                NotificationType.SYSTEM,
                null
            )
        } returns notification
        every { webSocketService.sendNotification(recipient.email, any()) } just Runs
        every { storageService.updateDeliveryStatus(notification) } just Runs

        assertDoesNotThrow {
            notificationService.sendNotificationAsActor(
                actorEmail = admin.email,
                recipientEmail = recipient.email,
                message = "hello",
                type = NotificationType.SYSTEM,
                groupId = null
            )
        }
    }

    @Test
    fun `sendNotificationAsActor should throw when non admin has no context`() {
        val actor =
            User(
                id = 7L,
                name = "Owner",
                email = "owner@example.com",
                passwordHash = "pw",
                role = UserRole.OWNER
            )
        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)

        val ex = assertFailsWith<UnauthorizedAccessException> {
            notificationService.sendNotificationAsActor(
                actorEmail = actor.email,
                recipientEmail = "recipient@example.com",
                message = "hello",
                type = NotificationType.GROUP,
                groupId = null
            )
        }

        assertEquals("Notification dispatch requires context (groupId) for non-admin users", ex.message)
    }

    @Test
    fun `sendNotificationAsActor should throw when actor has no board or project access`() {
        val actor =
            User(
                id = 8L,
                name = "Collaborator",
                email = "collab@example.com",
                passwordHash = "pw",
                role = UserRole.COLLABORATOR
            )
        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)
        every { boardRepository.findById(99L) } returns java.util.Optional.empty()
        every { projectRepository.findById(99L) } returns java.util.Optional.empty()
        every { taskRepository.findById(99L) } returns java.util.Optional.empty()

        val ex = assertFailsWith<UnauthorizedAccessException> {
            notificationService.sendNotificationAsActor(
                actorEmail = actor.email,
                recipientEmail = "recipient@example.com",
                message = "hello",
                type = NotificationType.GROUP,
                groupId = 99L
            )
        }

        assertEquals("Actor ${actor.email} has no contextual permission for groupId=99 and type=GROUP", ex.message)
    }

    @Test
    fun `sendNotificationAsActor should allow actor with board access`() {
        val actor =
            User(
                id = 9L,
                name = "Owner",
                email = "owner@example.com",
                passwordHash = "pw",
                role = UserRole.OWNER
            )
        val recipient =
            User(
                id = 10L,
                name = "Recipient",
                email = "recipient@example.com",
                passwordHash = "pw"
            )
        val board = mockk<Board>()
        every { board.owner } returns actor
        every { board.members } returns mutableSetOf()

        val notification =
            Notification(
                id = 301L,
                recipient = recipient,
                message = "board update",
                type = NotificationType.GROUP,
                groupId = 5L
            )

        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)
        every { boardRepository.findById(5L) } returns java.util.Optional.of(board)
        every { projectRepository.findById(5L) } returns java.util.Optional.empty()
        every { taskRepository.findById(5L) } returns java.util.Optional.empty()
        every {
            storageService.storeNotification(
                recipient.email,
                "board update",
                NotificationType.GROUP,
                5L
            )
        } returns notification
        every { webSocketService.sendNotification(recipient.email, any()) } just Runs
        every { storageService.updateDeliveryStatus(notification) } just Runs

        assertDoesNotThrow {
            notificationService.sendNotificationAsActor(
                actorEmail = actor.email,
                recipientEmail = recipient.email,
                message = "board update",
                type = NotificationType.GROUP,
                groupId = 5L
            )
        }
    }

    @Test
    fun `sendNotificationAsActor should allow actor with project member access`() {
        val actor =
            User(
                id = 11L,
                name = "Member",
                email = "member@example.com",
                passwordHash = "pw",
                role = UserRole.USER
            )
        val recipient =
            User(
                id = 12L,
                name = "Recipient",
                email = "recipient2@example.com",
                passwordHash = "pw"
            )
        val project = mockk<Project>()
        every { project.owner } returns recipient
        every { project.projectMembers } returns mutableSetOf(mockk { every { user } returns actor })

        val notification =
            Notification(
                id = 302L,
                recipient = recipient,
                message = "project update",
                type = NotificationType.GROUP,
                groupId = 77L
            )

        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)
        every { boardRepository.findById(77L) } returns java.util.Optional.empty()
        every { projectRepository.findById(77L) } returns java.util.Optional.of(project)
        every { taskRepository.findById(77L) } returns java.util.Optional.empty()
        every {
            storageService.storeNotification(
                recipient.email,
                "project update",
                NotificationType.GROUP,
                77L
            )
        } returns notification
        every { webSocketService.sendNotification(recipient.email, any()) } just Runs
        every { storageService.updateDeliveryStatus(notification) } just Runs

        assertDoesNotThrow {
            notificationService.sendNotificationAsActor(
                actorEmail = actor.email,
                recipientEmail = recipient.email,
                message = "project update",
                type = NotificationType.GROUP,
                groupId = 77L
            )
        }
    }

    @Test
    fun `sendNotificationAsActor should deny non member on task context`() {
        val actor = User(id = 30L, name = "User", email = "user@x.com", passwordHash = "pw", role = UserRole.USER)
        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)
        every { boardRepository.findById(555L) } returns java.util.Optional.empty()
        every { projectRepository.findById(555L) } returns java.util.Optional.empty()
        every { taskRepository.findById(555L) } returns java.util.Optional.empty()

        assertFailsWith<UnauthorizedAccessException> {
            notificationService.sendNotificationAsActor(
                actorEmail = actor.email,
                recipientEmail = "r@x.com",
                message = "denied",
                type = NotificationType.TASK_UPDATE,
                groupId = 555L
            )
        }
    }

    @Test
    fun `sendNotificationAsActor should allow member on task context`() {
        val actor = User(
            id = 31L,
            name = "Member",
            email = "member@x.com",
            passwordHash = "pw",
            role = UserRole.USER
        )
        val recipient =
            User(
                id = 32L,
                name = "Recipient",
                email = "recipient@x.com",
                passwordHash = "pw",
                role = UserRole.USER
            )
        val task = mockk<Task>()
        val board = mockk<Board>()

        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)
        every { boardRepository.findById(222L) } returns java.util.Optional.empty()
        every { projectRepository.findById(222L) } returns java.util.Optional.empty()
        every { taskRepository.findById(222L) } returns java.util.Optional.of(task)

        every { task.owner } returns recipient
        every { task.members } returns mutableSetOf(mockk { every { user } returns actor })
        every { task.board } returns board
        every { board.owner } returns recipient
        every { board.members } returns mutableSetOf()

        val notification = Notification(
            id = 401L,
            recipient = recipient,
            message = "ok",
            type = NotificationType.TASK_UPDATE,
            groupId = 222L
        )
        every {
            storageService.storeNotification(
                recipient.email,
                "ok",
                NotificationType.TASK_UPDATE,
                222L
            )
        } returns notification
        every { webSocketService.sendNotification(recipient.email, any()) } just Runs
        every { storageService.updateDeliveryStatus(notification) } just Runs

        assertDoesNotThrow {
            notificationService.sendNotificationAsActor(
                actorEmail = actor.email,
                recipientEmail = recipient.email,
                message = "ok",
                type = NotificationType.TASK_UPDATE,
                groupId = 222L
            )
        }
    }

    @Test
    fun `markAllAsRead should deny non admin cross user scope`() {
        val actor = User(
            id = 40L,
            name = "User",
            email = "user@x.com",
            passwordHash = "pw",
            role = UserRole.USER
        )
        every { userRepository.findByEmail(actor.email) } returns java.util.Optional.of(actor)

        assertFailsWith<UnauthorizedAccessException> {
            notificationService.markAllAsRead(actorEmail = actor.email, targetEmail = "other@x.com")
        }
    }

    @Test
    fun `markAllAsRead should allow admin cross user scope`() {
        val admin = User(
            id = 41L,
            name = "Admin",
            email = "admin@x.com",
            passwordHash = "pw",
            role = UserRole.ADMIN
        )
        every { userRepository.findByEmail(admin.email) } returns java.util.Optional.of(admin)
        every { storageService.markAllAsRead("other@x.com") } just Runs

        notificationService.markAllAsRead(actorEmail = admin.email, targetEmail = "other@x.com")

        verify(exactly = 1) { storageService.markAllAsRead("other@x.com") }
    }
}
