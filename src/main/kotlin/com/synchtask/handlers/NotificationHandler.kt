package com.synchtask.handlers

import com.synchtask.entities.NotificationType
import com.synchtask.user.domain.entity.User

interface NotificationHandler {

    fun supports(type: NotificationType): Boolean

    fun handle(user: User)
}
