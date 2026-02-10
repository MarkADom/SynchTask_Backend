package com.synchtask.notification.application.service

import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import org.springframework.stereotype.Service

@Service
class ActivityNotificationService(
    private val notificationService: NotificationService
) {

    fun handle(activity: Activity) {
        when (activity.type) {

            ActivityType.TASK_ASSIGNED -> {
                notifyTaskAssignment(activity)
            }

            ActivityType.TASK_COMMENTED -> {
                notifyTaskComment(activity)
            }

            ActivityType.PROJECT_CREATED -> {
                notifyProjectCreated(activity)
            }

            // outros ficam em branco por agora
            else -> Unit
        }
    }

    private fun notifyTaskAssignment(activity: Activity) {
        // TODO: here later we will pick up recipient
    }

    private fun notifyTaskComment(activity: Activity) {
        // TODO: here later we will pick up recipient
    }

    private fun notifyProjectCreated(activity: Activity) {
        // TODO: here later we will pick up recipient
    }
}
