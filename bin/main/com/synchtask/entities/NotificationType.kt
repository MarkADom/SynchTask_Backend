package com.synchtask.entities

/**
 * **NotificationType**
 *
 * Defines the different types of notifications in the system.
 *
 * - **PERSONAL** → Notifications directed at individual users (e.g., friend requests, direct messages).
 * - **GROUP** → Notifications related to shared tasks within a group.
 * - **SYSTEM** → Global system-wide notifications (e.g., new feature announcements).
 * - **TASK_UPDATE** -> Added for task-related notifications
 */
enum class NotificationType {
    PERSONAL,
    GROUP,
    SYSTEM,
    TASK_UPDATE,
    FRIEND_REQUEST,
    CHAT_MESSAGE,
    INVITATION
}

