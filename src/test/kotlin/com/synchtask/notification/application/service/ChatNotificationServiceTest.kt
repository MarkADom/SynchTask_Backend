package com.synchtask.notification.application.service

import com.synchtask.chat.application.dto.ChatNotificationDTO
import com.synchtask.notification.realtime.ChatNotificationService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate

class ChatNotificationServiceTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var service: ChatNotificationService

    @BeforeEach
    fun setup() {
        messagingTemplate = mockk()
        service = ChatNotificationService(messagingTemplate)
    }

    @Test
    fun `should send notification to recipient queue`() {
        // Given
        val dto = ChatNotificationDTO(
            recipientEmail = "bob@example.com",
            chatRoomId = 42L,
            message = "Encrypted message here"
        )
        every { messagingTemplate.convertAndSend("/queue/notifications/${dto.recipientEmail}", dto) } just runs

        // When
        service.notifyUser(dto)

        // Then
        verify(exactly = 1) {
            messagingTemplate.convertAndSend("/queue/notifications/bob@example.com", dto)
        }
    }
}
