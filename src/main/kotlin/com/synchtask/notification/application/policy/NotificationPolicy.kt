package com.synchtask.notification.application.policy

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.notification.domain.entity.NotificationType

/**
 * Defines how an Activity produces Notifications.
 *
 * Each implementation defines how notifications are generated.
 */
interface NotificationPolicy {
    fun supports(activity: Activity): Boolean

    fun buildMessage(activity: Activity): String

    fun notificationType(): NotificationType

    fun resolveRecipients(activity: Activity, contextSnapshot: ActivityContextSnapshot? = null): Set<String>
}
