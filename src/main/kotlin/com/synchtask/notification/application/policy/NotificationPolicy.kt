package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.notification.domain.entity.NotificationType
import com.synchtask.user.domain.entity.User

/**
 * Defines how an Activity produces Notifications.
 *
 * Each implementation defines how notifications are generated.
 */
interface NotificationPolicy {
    fun supports(activity: Activity): Boolean
    fun resolveRecipients(activity: Activity, contextSnapshot: ActivityContextSnapshot? = null): Set<User>
    fun buildMessage(activity: Activity): String
    fun notificationType(): NotificationType
}
