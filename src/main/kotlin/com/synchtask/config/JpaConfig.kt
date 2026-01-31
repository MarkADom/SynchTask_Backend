package com.synchtask.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Central JPA setup for repository scanning and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = ["com.synchtask.repositories"])
@EnableTransactionManagement
class JpaConfig {

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(entityManagerFactory).apply {
            // Optional: Additional settings (e.g., entityManagerFactory.unwrap(SessionFactory::class.java))
        }
}
