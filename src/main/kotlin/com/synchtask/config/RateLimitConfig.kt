package com.synchtask.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Central rate limiting setup using Bucket4j.
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

    fun resolveBucket(identifier: String): Bucket {
        return buckets.computeIfAbsent(identifier) {
            val (limit, window) =
                if (identifier.startsWith("anon-")) {
                    anonRequestLimit to anonTimeWindow
                } else {
                    userRequestLimit to userTimeWindow
                }

            Bucket.builder()
                .addLimit(
                    Bandwidth.classic(
                        limit,
                        Refill.greedy(limit, Duration.ofMinutes(window))
                    )
                )
                .build()
        }
    }

    @Bean
    fun publicBucket(): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.classic(
                publicRequestLimit,
                Refill.greedy(
                    publicRequestLimit,
                    Duration.ofMinutes(publicTimeWindow)
                )
            )
        )
        .build()
}
