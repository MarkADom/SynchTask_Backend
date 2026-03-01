package com.synchtask.task.presentation.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.Cache
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CacheAdminControllerTest {
    @Test
    fun `clear evicts cache and returns success message`() {
        val entityManager = mockk<EntityManager>()
        val entityManagerFactory = mockk<EntityManagerFactory>()
        val cache = mockk<Cache>(relaxed = true)

        every { entityManager.entityManagerFactory } returns entityManagerFactory
        every { entityManagerFactory.cache } returns cache

        val controller = CacheAdminController(entityManager)

        val response = controller.clear()

        assertEquals(200, response.statusCode.value())
        assertEquals("Cache cleared", response.body?.message)
        verify(exactly = 1) { cache.evictAll() }
    }
}
