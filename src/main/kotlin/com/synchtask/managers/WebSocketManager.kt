package com.synchtask.managers

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class WebSocketManager(
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)

    fun registerUser(userEmail: String) {
        redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, userEmail)
        redisTemplate.expire(ACTIVE_USERS_KEY, USER_EXPIRATION_MINUTES, TimeUnit.MINUTES)

        // Publish the user connection event to other servers
        redisTemplate.convertAndSend(USER_STATUS_TOPIC, "$userEmail:CONNECTED")

        logger.info("User connected: $userEmail")
    }

    fun unregisterUser(userEmail: String) {
        redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY, userEmail)

        // Publish the user disconnection event
        redisTemplate.convertAndSend(USER_STATUS_TOPIC, "$userEmail:DISCONNECTED")
        logger.info("User disconnected: $userEmail")
    }

    fun isUserOnline(userEmail: String): Boolean {
        return redisTemplate.opsForSet().isMember(ACTIVE_USERS_KEY, userEmail) == true
    }

    fun getActiveUsers(): Set<String> {
        return redisTemplate.opsForSet().members(ACTIVE_USERS_KEY) ?: emptySet()
    }

    companion object {
        private const val ACTIVE_USERS_KEY = "websocket:activeUsers"
        private const val USER_EXPIRATION_MINUTES = 30L
        private const val USER_STATUS_TOPIC = "websocket:userStatus"
    }
}
