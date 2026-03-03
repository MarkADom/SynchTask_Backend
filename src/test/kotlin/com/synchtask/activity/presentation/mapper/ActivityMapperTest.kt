
package com.synchtask.activity.presentation.mapper

import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.user.domain.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActivityMapperTest {
    private val actor = User(
        id = 1L,
        name = "Actor",
        email = "actor@test.com",
        passwordHash = "hash"
    )

    @Test
    fun `toResponse maps all fields`() {
        val activity = Activity(
            id = 10L,
            actor = actor,
            type = ActivityType.TASK_CREATED,
            referenceId = 77L,
            description = "Task created"
        )

        val response = ActivityMapper.toResponse(activity)

        assertEquals(10L, response.id)
        assertEquals("TASK_CREATED", response.type)
        assertEquals("actor@test.com", response.actorEmail)
        assertEquals(77L, response.referenceId)
        assertEquals("Task created", response.description)
    }

    @Test
    fun `toResponse throws when id is null`() {
        val activity = Activity(
            actor = actor,
            type = ActivityType.TASK_CREATED
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            ActivityMapper.toResponse(activity)
        }
        assertEquals("Activity ID cannot be null", exception.message)
    }
}
