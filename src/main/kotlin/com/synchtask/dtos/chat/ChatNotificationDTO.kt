package com.synchtask.dtos.chat

data class ChatNotificationDTO(
    val recipientEmail: String,
    val chatRoomId: Long,
    val message: String
)
