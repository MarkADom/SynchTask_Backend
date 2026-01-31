package com.synchtask.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * **Rate Limit Configuration**
 *
 * - Implements API rate limiting per **user** and **IP address**.
 * - Uses **Bucket4j** for in-memory rate limiting.
 * - Supports **dynamic rate limiting** for unauthenticated users.
 */
@Configuration
class RateLimitConfig(
    @Value("\${ratelimiter.user.limit}") private val userRequestLimit: Long,
    @Value("\${ratelimiter.user.window}") private val userTimeWindow: Long,
    @Value("\${ratelimiter.anon.limit}") private val anonRequestLimit: Long,
    @Value("\${ratelimiter.anon.window}") private val anonTimeWindow: Long,
    @Value("\${ratelimiter.public.limit}") private val publicRequestLimit: Long,
    @Value("\${ratelimiter.public.window}") private val publicTimeWindow: Long,
) {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    /**
     * **Resolves a rate-limiting bucket for a specific user or IP.**
     *
     * - If **authenticated**, applies user-specific limits.
     * - If **anonymous**, applies **stricter** limits to prevent abuse.
     */
    fun resolveBucket(identifier: String): Bucket {
        return buckets.computeIfAbsent(identifier) {
            val (limit, window) = if (identifier.startsWith("anon-")) {
                anonRequestLimit to anonTimeWindow
            } else {
                userRequestLimit to userTimeWindow
            }

            Bucket.builder()
                .addLimit(Bandwidth.classic(limit, Refill.greedy(limit, Duration.ofMinutes(window))))
                .build()
        }
    }

    /**
     * **Bucket for public API endpoints (higher rate limits).**
     */
    @Bean
    fun publicBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.classic(
                    publicRequestLimit,
                    Refill.greedy(publicRequestLimit, Duration.ofMinutes(publicTimeWindow))
                )
            )
            .build()

    /**
     * **Identifies the user or uses the IP if anonymous.**
     *
     * - Uses **email/username** for authenticated users.
     * - Uses **IP address** for unauthenticated users.
     */
    private fun getUserIdentifier(request: HttpServletRequest): String {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.isAuthenticated == true) {
            authentication.name
        } else {
            "anon-${request.remoteAddr}"
        }
    }
}
