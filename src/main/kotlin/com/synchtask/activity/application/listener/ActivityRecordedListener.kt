package com.synchtask.activity.application.listener

import com.synchtask.activity.application.event.ActivityRecordedEvent
import com.synchtask.notification.application.service.ActivityNotificationService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ActivityRecordedListener(
    private val activityNotificationService: ActivityNotificationService
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onActivityRecorded(event: ActivityRecordedEvent) {
        activityNotificationService.handle(event.activity, event.contextSnapshot)
    }
}
