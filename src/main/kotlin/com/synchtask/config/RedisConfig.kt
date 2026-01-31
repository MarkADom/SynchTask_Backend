package com.synchtask.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.synchtask.dtos.notification.NotificationRedisDTO
import com.synchtask.services.redis.RedisSubscriber
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis setup for caching and Pub/Sub messaging.
 */
@Configuration
class RedisConfig(
    @Value("\${spring.redis.host}") private val redisHost: String,
    @Value("\${spring.redis.port}") private val redisPort: Int,
    @Value("\${spring.redis.password}") private val redisPassword: String,
    @Value("\${spring.redis.database}") private val redisDatabase: Int,
    @Value("\${spring.redis.pubsub.chat-topic}") private val chatTopic: String,
    @Value("\${spring.redis.pubsub.notification-topic}") private val notificationTopic: String,
    @Value("\${spring.redis.pubsub.task-topic}") private val taskTopic: String,
) {

    private val logger = LoggerFactory.getLogger(RedisConfig::class.java)

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration(redisHost, redisPort)
        config.database = redisDatabase
        if (redisPassword.isNotEmpty()) {
            config.password = RedisPassword.of(redisPassword)
        }

        val poolingConfig = LettucePoolingClientConfiguration.builder().build()
        return LettuceConnectionFactory(config, poolingConfig)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, NotificationRedisDTO> {
        val template = RedisTemplate<String, NotificationRedisDTO>()
        template.connectionFactory = connectionFactory

        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule()) // Needed for LocalDateTime
            findAndRegisterModules()
        }

        val serializer = Jackson2JsonRedisSerializer(objectMapper, NotificationRedisDTO::class.java)

        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.valueSerializer = serializer
        template.hashValueSerializer = serializer
        template.afterPropertiesSet()

        return template
    }

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        redisSubscriber: RedisSubscriber,
    ): RedisMessageListenerContainer =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            addMessageListener(messageListenerAdapter(redisSubscriber), PatternTopic(chatTopic))
            addMessageListener(messageListenerAdapter(redisSubscriber), PatternTopic(notificationTopic))
            addMessageListener(messageListenerAdapter(redisSubscriber), PatternTopic(taskTopic))
        }

    @Bean
    fun messageListenerAdapter(subscriber: RedisSubscriber): MessageListenerAdapter =
        MessageListenerAdapter(subscriber, "handleMessage")

    @PostConstruct
    fun logConfig() {
        logger.info("Redis Configured:")
        logger.info(" - Host: {}", redisHost)
        logger.info(" - Port: {}", redisPort)
        logger.info(" - Database: {}", redisDatabase)
        logger.info(" - Chat Topic: {}", chatTopic)
        logger.info(" - Notification Topic: {}", notificationTopic)
    }
}
