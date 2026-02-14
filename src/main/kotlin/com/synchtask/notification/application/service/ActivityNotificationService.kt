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
                val recipientEmails = policy.resolveRecipients(activity, contextSnapshot)

                recipientEmails.forEach { email ->
                    notificationService.sendNotification(
                        userEmail = email,
                        message = policy.buildMessage(activity),
                        type = policy.notificationType(),
                        groupId = activity.referenceId
                    )
                }
            }
    }
}
