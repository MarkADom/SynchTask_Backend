package com.synchtask.activity.application.service

import com.synchtask.activity.application.event.ActivityContextSnapshot
import com.synchtask.activity.application.event.ActivityRecordedEvent
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.activity.domain.repository.ActivityRepository
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class ActivityServiceTest {
    private lateinit var repository: ActivityRepository
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var service: ActivityService

    private val actor = User(
        id = 1L,
        name = "Actor",
        email = "actor@test.com",
        passwordHash = "hash"
    )

    @BeforeEach
    fun setup() {
        repository = mockk()
        eventPublisher = mockk(relaxed = true)
        service = ActivityService(repository, eventPublisher)
    }

    @Test
    fun `record saves activity and publishes event`() {
        val snapshot = ActivityContextSnapshot(ownerEmail = "owner@test.com")
        val activitySlot = slot<Activity>()
        every { repository.save(capture(activitySlot)) } answers {
            Activity(
                id = 100L,
                actor = activitySlot.captured.actor,
                type = activitySlot.captured.type,
                referenceId = activitySlot.captured.referenceId,
                description = activitySlot.captured.description
            )
        }

        val saved = service.record(actor, ActivityType.TASK_UPDATED, 9L, "Updated", snapshot)

        assertEquals(100L, saved.id)
        assertEquals(ActivityType.TASK_UPDATED, activitySlot.captured.type)
        verify(exactly = 1) {
            eventPublisher.publishEvent(
                withArg<ActivityRecordedEvent> {
                    assertEquals(saved.id, it.activity.id)
                    assertEquals(snapshot, it.contextSnapshot)
                }
            )
        }
    }

    @Test
    fun `getActivitiesForUser delegates to repository`() {
        every { repository.findAllByActorOrderByCreatedAtDesc(actor) } returns emptyList()

        val result = service.getActivitiesForUser(actor)

        assertEquals(0, result.size)
        verify(exactly = 1) { repository.findAllByActorOrderByCreatedAtDesc(actor) }
    }

    @Test
    fun `getActivitiesForReference delegates to repository`() {
        every { repository.findAllByReferenceIdOrderByCreatedAtDesc(12L) } returns emptyList()

        val result = service.getActivitiesForReference(12L)

        assertEquals(0, result.size)
        verify(exactly = 1) { repository.findAllByReferenceIdOrderByCreatedAtDesc(12L) }
    }
}
