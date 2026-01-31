package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User

/**
 * Interface for handling different types of notifications.
 */
interface NotificationHandler {

    /**
     * Returns true if this handler supports the given notification type.
     */
    fun supports(type: NotificationType): Boolean

    /**
     * Processes the notification logic for the given user.
     */
    fun handle(user: User)
}
