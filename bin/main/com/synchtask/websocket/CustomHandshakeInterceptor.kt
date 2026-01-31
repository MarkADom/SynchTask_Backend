package com.synchtask.websocket

import com.synchtask.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * **WebSocket Handshake Interceptor**
 *
 * - Extracts and validates JWT tokens from **headers** or **query parameters**.
 * - Ensures only authenticated users establish WebSocket connections.
 */
class CustomHandshakeInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
) : HandshakeInterceptor {

    private val logger = LoggerFactory.getLogger(CustomHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest, response: ServerHttpResponse,
        wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>,
    ): Boolean {
        logger.info("Intercepting WebSocket handshake...")

        val token = extractToken(request) ?: return rejectConnection("Missing authentication token")

        val userDetails: UserDetails = jwtTokenProvider.validateAndExtractUser(token)
            ?: return rejectConnection("Invalid or expired JWT token")

        attributes["username"] = userDetails.username
        logger.info("WebSocket authentication successful for user: ${userDetails.username}")
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        logger.info("WebSocket connection established.")
    }

    /**
     * **Extracts JWT from request headers or query parameters**
     *
     * - First checks the `Authorization` header for a Bearer token.
     * - If not found, looks for `token` in query parameters.
     *
     * @param request Incoming WebSocket HTTP request.
     * @return The JWT token or `null` if not found.
     */
    /**
     * **Extracts JWT token from query parameters only**
     *
     * Logs clearly whether a token was found or not, and its origin.
     *
     * @param request The incoming WebSocket handshake request.
     * @return JWT token string if present, or `null`.
     */
    private fun extractToken(request: ServerHttpRequest): String? {
        val query = request.uri.query

        if (query.isNullOrBlank()) {
            logger.warn("No query parameters present in WebSocket handshake request")
            return null
        }

        val token = query.split("&")
            .mapNotNull { param ->
                val (key, value) = param.split("=").takeIf { it.size == 2 } ?: return@mapNotNull null
                key to value
            }
            .toMap()
            .get("token")

        if (token.isNullOrBlank()) {
            logger.warn("JWT token not found in query parameters")
        } else {
            logger.info("JWT token successfully extracted from query parameters")
        }

        return token
    }

    /**
     * **Rejects a WebSocket connection due to authentication failure**
     *
     * @param reason The reason for rejection.
     * @return Always returns `false` to indicate failure.
     */
    private fun rejectConnection(reason: String): Boolean {
        logger.warn("WebSocket connection rejected: $reason")
        return false
    }
}
