package com.synchtask.managers

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * **WebSocketManager**
 *
 * - Manages active WebSocket connections across multiple servers using Redis.
 * - Ensures distributed tracking of online users.
 * - Helps route messages efficiently by keeping track of connected users.
 */
@Service
class WebSocketManager(
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)

    /**
     * **Registers a new WebSocket connection**
     *
     * - Adds the user to the active users list in Redis.
     * - Sets an expiration time to automatically clean up inactive users.
     *
     * @param userEmail The email of the connected user.
     */
    fun registerUser(userEmail: String) {
        redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, userEmail)
        redisTemplate.expire(ACTIVE_USERS_KEY, USER_EXPIRATION_MINUTES, TimeUnit.MINUTES)

        // Publish the user connection event to other servers
        redisTemplate.convertAndSend(USER_STATUS_TOPIC, "$userEmail:CONNECTED")

        logger.info("User connected: $userEmail")
    }

    /**
     * **Unregisters a WebSocket connection**
     * - Removes the user from the active users list in Redis.
     *
     * @param userEmail The email of the disconnected user.
     */
    fun unregisterUser(userEmail: String) {
        redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY, userEmail)

        // Publish the user disconnection event
        redisTemplate.convertAndSend(USER_STATUS_TOPIC, "$userEmail:DISCONNECTED")
        logger.info("User disconnected: $userEmail")
    }

    /**
     * **Checks if a user is online**
     *
     * - Verifies if the user is currently present in the Redis active users set.
     * @param userEmail The email of the user.
     * @return `true` if the user is online, `false` otherwise.
     */
    fun isUserOnline(userEmail: String): Boolean {
        return redisTemplate.opsForSet().isMember(ACTIVE_USERS_KEY, userEmail) == true
    }

    /**
     * **Gets all active users**
     *
     * - Retrieves the set of all currently online users.
     * @return A set of user emails who are currently online.
     */
    fun getActiveUsers(): Set<String> {
        return redisTemplate.opsForSet().members(ACTIVE_USERS_KEY) ?: emptySet()
    }

    companion object {
        private const val ACTIVE_USERS_KEY = "websocket:activeUsers"
        private const val USER_EXPIRATION_MINUTES = 30L
        private const val USER_STATUS_TOPIC = "websocket:userStatus"
    }
}
