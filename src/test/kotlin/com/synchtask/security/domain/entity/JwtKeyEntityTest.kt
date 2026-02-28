package com.synchtask.security.domain.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class JwtKeyEntityTest {
    @Test
    fun `constructor stores provided values`() {
        val entity = JwtKeyEntity(
            id = 12L,
            privateKey = "private-key"
        )

        assertEquals(12L, entity.id)
        assertEquals("private-key", entity.privateKey)
        assertNotNull(entity.createdAt)
    }
}
