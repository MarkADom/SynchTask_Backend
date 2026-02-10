package com.synchtask.activity.application.service

import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.activity.domain.repository.ActivityRepository
import com.synchtask.notification.application.service.ActivityNotificationService
import com.synchtask.user.domain.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivityService(
    private val activityRepository: ActivityRepository,
    private val activityNotificationService: ActivityNotificationService,
) {

    @Transactional
    fun record(
        actor: User,
        type: ActivityType,
        referenceId: Long? = null,
        description: String? = null,
    ): Activity {
        val activity = activityRepository.save(
            Activity(
                actor = actor,
                type = type,
                referenceId = referenceId,
                description = description
            )
        )
        activityNotificationService.handle(activity)

        return activity
    }

    fun getActivitiesForUser(user: User): List<Activity> {
        return activityRepository.findAllByActorOrderByCreatedAtDesc(user)
    }

    fun getActivitiesForReference(referenceId: Long): List<Activity> {
        return activityRepository.findAllByReferenceIdOrderByCreatedAtDesc(referenceId)
    }
}
