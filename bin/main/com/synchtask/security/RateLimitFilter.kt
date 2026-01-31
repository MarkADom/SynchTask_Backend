package com.synchtask.security

import com.synchtask.config.RateLimitConfig
import io.github.bucket4j.Bucket
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate Limit Filter
 *
 * Applies rate limiting based on authenticated user or IP address.
 * Disabled in development profile to avoid blocking during active development.
 */
@Component
class RateLimitFilter(
    private val rateLimitConfig: RateLimitConfig,
    private val environment: Environment
) : Filter {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)
    private val userBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val requestURI = httpRequest.requestURI

        // Skip rate limiting for public endpoints
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response)
            return
        }

        // Skip rate limiting in development profile
        if (environment.activeProfiles.contains("dev")) {
            // TODO: Enable rate limiting for production environments only
            chain.doFilter(request, response)
            return
        }

        val identifier = getUserOrIp(httpRequest)
        val bucket = userBuckets.computeIfAbsent(identifier) {
            rateLimitConfig.resolveBucket(identifier)
        }

        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for: $identifier - URI: $requestURI")
            httpResponse.status = HTTP_TOO_MANY_REQUESTS
            httpResponse.setHeader("Retry-After", RATE_LIMIT_RETRY_AFTER.toString())
            httpResponse.writer.write(RATE_LIMIT_MESSAGE)
            return
        }

        chain.doFilter(request, response)
    }

    private fun getUserOrIp(request: HttpServletRequest): String {
        return SecurityContextHolder.getContext()?.authentication?.let { auth ->
            if (auth.isAuthenticated) auth.name else getClientIp(request)
        } ?: getClientIp(request)
    }

    private fun isPublicEndpoint(uri: String): Boolean {
        return PUBLIC_ENDPOINTS.any { uri.startsWith(it) }
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return forwardedFor?.split(",")?.map { it.trim() }
            ?.firstOrNull { ip -> !isPrivateIp(ip) }
            ?: request.remoteAddr
    }

    private fun isPrivateIp(ip: String): Boolean {
        return PRIVATE_IP_PREFIXES.any { ip.startsWith(it) }
    }

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val RATE_LIMIT_MESSAGE = "Rate limit exceeded. Try again later."
        private const val RATE_LIMIT_RETRY_AFTER = 60

        private val PUBLIC_ENDPOINTS = listOf(
            "/auth/login",
            "/auth/register",
            "/actuator"
        )

        private val PRIVATE_IP_PREFIXES = listOf(
            "10.", "192.168.", "172.16.", "127.", "169.254."
        )
    }
}
