package com.synchtask.notification.application.event

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class NotificationEventTest {
    @Test
    fun `can instantiate notification event`() {
        val event = NotificationEvent()

        assertNotNull(event)
    }
}
