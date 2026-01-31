package com.synchtask.config

import com.synchtask.services.redis.RedisSubscriber
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RedisConfigTest {

    private val redisHost = "localhost"
    private val redisPort = 6379
    private val redisPassword = "secret"
    private val redisDatabase = 0
    private val chatTopic = "chat"
    private val notificationTopic = "notification"
    private val taskTopic = "task"

    private val redisConfig = RedisConfig(
        redisHost,
        redisPort,
        redisPassword,
        redisDatabase,
        chatTopic,
        notificationTopic,
        taskTopic
    )

    @Test
    fun `should create RedisConnectionFactory`() {
        val factory = redisConfig.redisConnectionFactory()
        assertNotNull(factory)
    }

    @Test
    fun `should create configured RedisTemplate`() {
        val connectionFactory = mockk<RedisConnectionFactory>(relaxed = true)
        val template = redisConfig.redisTemplate(connectionFactory)

        assertNotNull(template)
        assert(template.keySerializer is StringRedisSerializer)
        assert(template.valueSerializer is Jackson2JsonRedisSerializer<*>)
    }

    @Test
    fun `should create MessageListenerAdapter with correct method`() {
        val subscriber = mockk<RedisSubscriber>(relaxed = true)
        val adapter = redisConfig.messageListenerAdapter(subscriber)

        assertNotNull(adapter)
    }

    @Test
    fun `should configure RedisMessageListenerContainer with topics`() {
        val connectionFactory = mockk<RedisConnectionFactory>(relaxed = true)
        val subscriber = mockk<RedisSubscriber>(relaxed = true)

        val container: RedisMessageListenerContainer =
            redisConfig.redisMessageListenerContainer(connectionFactory, subscriber)

        assertNotNull(container)
        assertEquals(connectionFactory, container.connectionFactory)
    }
}
