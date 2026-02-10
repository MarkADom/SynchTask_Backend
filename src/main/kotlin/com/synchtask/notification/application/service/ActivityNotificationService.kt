package com.synchtask.notification.application.service

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.notification.application.policy.NotificationPolicy
import org.springframework.stereotype.Service

@Service
class ActivityNotificationService(
    private val notificationPolicies: List<NotificationPolicy>,
    private val notificationService: NotificationService,
) {

    fun handle(activity: Activity, contextSnapshot: ActivityContextSnapshot? = null) {
        notificationPolicies
            .filter { it.supports(activity) }
            .forEach { policy ->
                val recipients = policy.resolveRecipients(activity, contextSnapshot)

                recipients.forEach { user ->
                    notificationService.sendNotification(
                        userEmail = user.email,
                        message = policy.buildMessage(activity),
                        type = policy.notificationType(),
                        groupId = activity.referenceId
                    )
                }
            }
    }
}
