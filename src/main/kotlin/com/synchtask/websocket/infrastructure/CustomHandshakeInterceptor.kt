package com.synchtask.websocket.infrastructure

import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

class CustomHandshakeInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
) : HandshakeInterceptor {
    private val logger = LoggerFactory.getLogger(CustomHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        logger.info("Intercepting WebSocket handshake...")

        val token = extractToken(request) ?: return rejectConnection("Missing authentication token")

        val userDetails: UserDetails =
            jwtTokenProvider.validateAndExtractUser(token)
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

    private fun extractToken(request: ServerHttpRequest): String? {
        val authHeader = request.headers.getFirst("Authorization")
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ").trim()
            if (token.isNotBlank()) {
                logger.info("JWT token extracted from Authorization header")
                return token
            }
        }

        val query = request.uri.query ?: return null

        val token =
            query.split("&").firstNotNullOfOrNull { param ->
                val parts = param.split("=")
                if (parts.size == 2 && parts[0] == "token") parts[1] else null
            }

        if (token.isNullOrBlank()) {
            logger.warn("JWT token not found in query parameters")
            return null
        } else {
            logger.info("JWT token successfully extracted from query parameters")
        }
        return token
    }

    private fun rejectConnection(reason: String): Boolean {
        logger.warn("WebSocket connection rejected: $reason")
        return false
    }
}
