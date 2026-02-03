package com.synchtask.controllers

import com.synchtask.notification.application.dto.NotificationDTO
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.notification.presentation.controller.NotificationWebSocketController
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

/**
 * **NotificationWebSocketControllerTest**
 *
 * Unit test for secure WebSocket notification dispatch.
 */
class NotificationWebSocketControllerTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var controller: NotificationWebSocketController

    @BeforeEach
    fun setUp() {
        messagingTemplate = mockk(relaxed = true)
        controller = NotificationWebSocketController(messagingTemplate)

        val securityContext = mockk<SecurityContext>()
        val authentication = UsernamePasswordAuthenticationToken("user@email.com", null)
        every { securityContext.authentication } returns authentication
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `should send notification to authorized user`() {
        val notification = NotificationDTO(
            id = 1L,
            recipientEmail = "user@email.com",
            message = "You have a new task assigned.",
            type = NotificationType.PERSONAL,
            timestamp = LocalDateTime.now(),
            read = false
        )

        controller.notifyUser("user@email.com", notification)

        verify(exactly = 1) {
            messagingTemplate.convertAndSendToUser("user@email.com", "/queue/notifications", notification)
        }
    }

    @Test
    fun `should not send notification to unauthorized user`() {
        val notification = NotificationDTO(
            id = 2L,
            recipientEmail = "attacker@email.com",
            message = "Unauthorized attempt!",
            type = NotificationType.SYSTEM,
            timestamp = LocalDateTime.now(),
            read = false
        )

        controller.notifyUser("attacker@email.com", notification)

        verify(exactly = 0) {
            messagingTemplate.convertAndSendToUser(any(), any(), any())
        }
    }
}
