package com.synchtask.notification.application.handler

import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User

interface NotificationHandler {

    fun supports(type: NotificationType): Boolean

    fun handle(user: User)
}
