package com.synchtask.notification.presentation.controller

import com.synchtask.redis.application.service.NotificationRedisCleanupService
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class NotificationMaintenanceControllerTest {
    private val redisCleanupService = mockk<NotificationRedisCleanupService>()
    private val controller = NotificationMaintenanceController(redisCleanupService)

    @Test
    fun `should trigger manual redis cleanup`() {
        io.mockk.every { redisCleanupService.cleanOldNotifications() } just runs

        val response = controller.triggerRedisCleanup()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Manual Redis notification cleanup triggered.", response.body?.message)
        verify(exactly = 1) { redisCleanupService.cleanOldNotifications() }
    }
}
