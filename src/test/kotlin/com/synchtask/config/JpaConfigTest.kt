package com.synchtask.config

import io.mockk.mockk
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager

class JpaConfigTest {
    private val jpaConfig = JpaConfig()

    @Test
    fun `should create JpaTransactionManager bean`() {
        // Arrange
        val entityManagerFactory = mockk<EntityManagerFactory>(relaxed = true)

        // Act
        val transactionManager: PlatformTransactionManager =
            jpaConfig.transactionManager(entityManagerFactory)

        // Assert
        assertNotNull(transactionManager)
        assert(transactionManager is JpaTransactionManager)
    }
}
