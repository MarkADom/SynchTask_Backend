package com.synchtask.redis.application.handler

import com.synchtask.shared.application.handler.RedisRetryHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.ScheduledExecutorService

class RedisRetryHandlerTest {

    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var scheduler: ScheduledExecutorService
    private lateinit var handler: RedisRetryHandler

    @BeforeEach
    fun setup() {
        redisTemplate = mockk()
        scheduler = mockk(relaxed = true)

        handler = spyk(RedisRetryHandler(redisTemplate), recordPrivateCalls = true)

        handler.apply {
            val field = this::class.java.getDeclaredField("scheduler")
            field.isAccessible = true
            field.set(this, scheduler)
        }
    }

    @Test
    fun `should publish message to redis on first attempt`() {
        every { redisTemplate.convertAndSend("test-channel", "hello") } returns 1L

        handler.retryMessagePublishing("test-channel", "hello", 1)

        verify(exactly = 1) {
            scheduler.schedule(any(), any(), any())
        }

        val slot = slot<Runnable>()
        verify {
            scheduler.schedule(capture(slot), any(), any())
        }
        slot.captured.run()

        verify {
            redisTemplate.convertAndSend("test-channel", "hello")
        }
    }

    @Test
    fun `should retry message on redis connection failure`() {
        every {
            redisTemplate.convertAndSend(
                "test-channel",
                "fail"
            )
        } throws RedisConnectionFailureException("Simulated")

        handler.retryMessagePublishing("test-channel", "fail", 1)

        verify(atLeast = 1) {
            scheduler.schedule(any(), any(), any())
        }
    }

    @Test
    fun `should stop retrying after max attempts`() {
        every {
            redisTemplate.convertAndSend(
                "test-channel",
                "fail"
            )
        } throws RedisConnectionFailureException("Simulated")

        handler.retryMessagePublishing("test-channel", "fail", 5)

        verify(exactly = 1) {
            scheduler.schedule(any(), any(), any())
        }
    }
}
