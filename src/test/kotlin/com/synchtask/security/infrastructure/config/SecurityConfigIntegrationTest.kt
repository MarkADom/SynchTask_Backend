package com.synchtask.security.infrastructure.config

import com.synchtask.config.RateLimitConfig
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
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertTrue

@WebMvcTest(controllers = [SecurityConfigTestController::class])
class SecurityConfigIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

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
    fun `oauth2 authorization endpoint should be publicly accessible`() {
        mockMvc.perform(get("/oauth2/authorization/google"))
            .andExpect { result ->
                assertTrue(result.response.status !in setOf(401, 403))
            }
    }

    @Test
    fun `notifications endpoint should require authentication`() {
        mockMvc.perform(get("/notifications/test"))
            .andExpect { result ->
                assertTrue(result.response.status in setOf(401, 403, 302))
            }
    }

    @Test
    fun `boards endpoint should require authentication`() {
        mockMvc.perform(get("/boards"))
            .andExpect { result ->
                assertTrue(result.response.status in setOf(401, 403, 302))
            }
    }

    @Test
    fun `boards endpoint should allow authenticated bearer jwt request`() {
        mockMvc.perform(
            get("/boards")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
        )
            .andExpect(status().isOk)
    }
}

@RestController
class SecurityConfigTestController {
    @GetMapping("/auth/login")
    fun loginPublic(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("status" to "UP"))

    @GetMapping("/notifications/test")
    fun notifications(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("notifications" to "ok"))

    @GetMapping("/boards")
    fun boards(): ResponseEntity<List<String>> =
        ResponseEntity.ok(listOf("b1"))
}
