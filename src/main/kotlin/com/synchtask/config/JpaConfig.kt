package com.synchtask.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.autoconfigure.domain.EntityScan
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
@EnableJpaRepositories(
    basePackages = [
        "com.synchtask.task.domain.repository",
        "com.synchtask.user.domain.repository",
        "com.synchtask.board.domain.repository",
        "com.synchtask.project.domain.repository",
        "com.synchtask.notification.domain.repository",
        "com.synchtask.chat.domain.repository",
        "com.synchtask.friend.domain.repository",
        "com.synchtask.security.domain.repository",
        "com.synchtask.activity.domain.repository",
    ]
)
@EntityScan(
    basePackages = [
        "com.synchtask.task.domain.entity",
        "com.synchtask.user.domain.entity",
        "com.synchtask.board.domain.entity",
        "com.synchtask.project.domain.entity",
        "com.synchtask.notification.domain.entity",
        "com.synchtask.chat.domain.entity",
        "com.synchtask.friend.domain.entity",
        "com.synchtask.security.domain.entity",
        "com.synchtask.activity.domain.entity",
    ]
)


@EnableTransactionManagement
class JpaConfig {

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(entityManagerFactory).apply {
            // Optional: Additional settings (e.g., entityManagerFactory.unwrap(SessionFactory::class.java))
        }
}
