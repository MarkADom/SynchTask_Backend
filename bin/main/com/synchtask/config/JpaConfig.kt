package com.synchtask.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * **JPA Configuration**
 *
 * - Enables transaction management and repository scanning.
 * - Ensures correct transaction handling for JPA operations.
 */
@Configuration
@EnableJpaRepositories(basePackages = ["com.synchtask.repositories"])
@EnableTransactionManagement
class JpaConfig {

    /**
     * **Configures JPA transaction management.**
     *
     * - Ensures transaction safety and rollback consistency.
     * - Uses an explicitly defined `JpaTransactionManager` to manage transactions.
     */
    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(entityManagerFactory).apply {
            // Optional: Additional settings (e.g., entityManagerFactory.unwrap(SessionFactory::class.java))
        }
}
