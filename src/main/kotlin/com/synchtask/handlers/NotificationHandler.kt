package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.entities.User

interface NotificationHandler {

    fun supports(type: NotificationType): Boolean

    fun handle(user: User)
}
