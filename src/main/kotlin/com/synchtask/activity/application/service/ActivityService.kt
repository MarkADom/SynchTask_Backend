package com.synchtask.activity.application.service

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.application.event.ActivityRecordedEvent
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.activity.domain.repository.ActivityRepository
import com.synchtask.user.domain.entity.User
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivityService(
    private val activityRepository: ActivityRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun record(
        actor: User,
        type: ActivityType,
        referenceId: Long? = null,
        description: String? = null,
        contextSnapshot: ActivityContextSnapshot? = null,
    ): Activity {
        val activity =
            activityRepository.save(
                Activity(
                    actor = actor,
                    type = type,
                    referenceId = referenceId,
                    description = description
                )
            )

        eventPublisher.publishEvent(
            ActivityRecordedEvent(
                activity = activity,
                contextSnapshot = contextSnapshot
            )
        )

        return activity
    }

    fun getActivitiesForUser(user: User): List<Activity> {
        return activityRepository.findAllByActorOrderByCreatedAtDesc(user)
    }

    fun getActivitiesForReference(referenceId: Long): List<Activity> {
        return activityRepository.findAllByReferenceIdOrderByCreatedAtDesc(referenceId)
    }
}
