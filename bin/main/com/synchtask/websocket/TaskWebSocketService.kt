package com.synchtask.websocket

import com.synchtask.dtos.task.TaskResponseDTO
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

/**
 * Task WebSocket Service
 *
 * Sends real-time task updates to subscribed clients using STOMP over WebSocket.
 */
@Service
class TaskWebSocketService(
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(TaskWebSocketService::class.java)

    /**
     * Broadcasts task update to the topic `/topic/tasks/{taskId}`.
     *
     * @param task The updated task to broadcast.
     */
    fun sendTaskUpdate(task: TaskResponseDTO) {
        val topic = "/topic/tasks/${task.id}"
        messagingTemplate.convertAndSend(topic, task)
        logger.debug("Broadcasted task update on topic $topic: ${task.title}")
    }
}
