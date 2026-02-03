package com.synchtask.websocket

import com.synchtask.task.application.dto.TaskResponseDTO
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.websocket.application.service.TaskWebSocketService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.LocalDateTime

class TaskWebSocketServiceTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var service: TaskWebSocketService

    @BeforeEach
    fun setup() {
        messagingTemplate = mockk(relaxed = true)
        service = TaskWebSocketService(messagingTemplate)
    }

    @Test
    fun `should send task update to correct WebSocket topic`() {
        val task = TaskResponseDTO(
            id = 123L,
            title = "Test task",
            description = "This is a test task",
            creatorId = 1L,
            creatorName = "Owner",
            assignees = listOf(2L),
            status = TaskStatus.TODO,
            labels = listOf("urgent", "backend"),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            boardId = 10L,
            boardName = "Main Board",
            projectName = null,
            priority = TaskPriority.MID
        )

        service.sendTaskUpdate(task)

        verify {
            messagingTemplate.convertAndSend("/topic/tasks/123", task)
        }
    }
}
