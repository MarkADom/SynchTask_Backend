package com.synchtask.security.infrastructure.config

import com.synchtask.config.RateLimitConfig
import com.synchtask.security.infrastructure.filter.JwtAuthenticationFilter
import com.synchtask.security.infrastructure.filter.RateLimitFilter
import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@WebMvcTest(controllers = [SecurityConfigTestController::class])
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, RateLimitFilter::class)
class SecurityConfigIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockBean
    lateinit var rateLimitConfig: RateLimitConfig

    @MockBean
    lateinit var userDetailsService: UserDetailsService

    @BeforeEach
    fun setup() {
        val bucket =
            Bucket.builder()
                .addLimit(Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofMinutes(1))))
                .build()

        `when`(rateLimitConfig.resolveBucket(anyString())).thenReturn(bucket)
    }

    @Test
    fun `actuator health should be publicly accessible`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `actuator env should require authentication`() {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `boards endpoint should require authentication`() {
        mockMvc.perform(get("/boards"))
            .andExpect(status().isUnauthorized)
    }
}

@RestController
class SecurityConfigTestController {
    @GetMapping("/actuator/health")
    fun health(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("status" to "UP"))

    @GetMapping("/boards")
    fun boards(): ResponseEntity<List<String>> = ResponseEntity.ok(listOf("b1"))
}
