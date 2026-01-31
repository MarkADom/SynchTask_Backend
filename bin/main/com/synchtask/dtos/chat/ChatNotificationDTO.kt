package com.synchtask.dtos.chat

/**
 * **Chat Notification DTO**
 *
 * Represents a notification for a new chat message.
 */
data class ChatNotificationDTO(
    val recipientEmail: String,
    val chatRoomId: Long,
    val message: String
)
