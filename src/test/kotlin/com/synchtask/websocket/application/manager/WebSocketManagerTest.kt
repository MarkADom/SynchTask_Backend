package com.synchtask.websocket.application.manager

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebSocketManagerTest {
    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var setOps: SetOperations<String, String>
    private lateinit var manager: WebSocketManager

    @BeforeEach
    fun setup() {
        redisTemplate = mockk(relaxed = true)
        setOps = mockk(relaxed = true)

        every { redisTemplate.opsForSet() } returns setOps

        manager = WebSocketManager(redisTemplate)
    }

    @Test
    fun `should register user and set expiration and publish event`() {
        val email = "alice@example.com"

        manager.registerUser(email)

        verify {
            setOps.add("websocket:activeUsers", email)
            redisTemplate.expire("websocket:activeUsers", 30L, TimeUnit.MINUTES)
            redisTemplate.convertAndSend("websocket:userStatus", "$email:CONNECTED")
        }
    }

    @Test
    fun `should unregister user and publish event`() {
        val email = "bob@example.com"

        manager.unregisterUser(email)

        verify {
            setOps.remove("websocket:activeUsers", email)
            redisTemplate.convertAndSend("websocket:userStatus", "$email:DISCONNECTED")
        }
    }

    @Test
    fun `should return true if user is in Redis set`() {
        every { setOps.isMember("websocket:activeUsers", "charlie@example.com") } returns true

        val result = manager.isUserOnline("charlie@example.com")

        assertTrue(result)
    }

    @Test
    fun `should return false if user is not in Redis set`() {
        every { setOps.isMember("websocket:activeUsers", "ghost@example.com") } returns false

        val result = manager.isUserOnline("ghost@example.com")

        assertFalse(result)
    }

    @Test
    fun `should return active users from Redis`() {
        every { setOps.members("websocket:activeUsers") } returns setOf("u1@example.com", "u2@example.com")

        val result = manager.getActiveUsers()

        assertEquals(setOf("u1@example.com", "u2@example.com"), result)
    }

    @Test
    fun `should return empty set if Redis returns null`() {
        every { setOps.members("websocket:activeUsers") } returns null

        val result = manager.getActiveUsers()

        assertTrue(result.isEmpty())
    }
}
