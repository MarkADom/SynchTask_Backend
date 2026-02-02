package com.synchtask.chat.application.dto

data class ChatNotificationDTO(
    val recipientEmail: String,
    val chatRoomId: Long,
    val message: String
)
