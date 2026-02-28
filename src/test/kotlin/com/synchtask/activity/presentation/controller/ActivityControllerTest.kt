package com.synchtask.activity.presentation.controller

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.activity.domain.model.ActivityType
import com.synchtask.user.application.service.AuthenticatedUserService
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.User as SpringUser

class ActivityControllerTest {
    private val activityService = mockk<ActivityService>()
    private val authenticatedUserService = mockk<AuthenticatedUserService>()
    private val controller = ActivityController(activityService, authenticatedUserService)

    @Test
    fun `getMyActivities resolves authenticated user and maps response`() {
        val userEntity = User(
            id = 1L,
            name = "A",
            email = "a@test.com",
            passwordHash = "hash"
        )
        val principal = SpringUser("a@test.com", "pwd", emptyList())
        val activity = Activity(
            id = 7L,
            actor = userEntity,
            type = ActivityType.TASK_CREATED,
            referenceId = 20L,
            description = "Criada"
        )

        every { authenticatedUserService.requireUser(principal) } returns userEntity
        every { activityService.getActivitiesForUser(userEntity) } returns listOf(activity)

        val result = controller.getMyActivities(principal)

        assertEquals(1, result.size)
        assertEquals(7L, result.first().id)
        verify(exactly = 1) { authenticatedUserService.requireUser(principal) }
        verify(exactly = 1) { activityService.getActivitiesForUser(userEntity) }
    }
}
