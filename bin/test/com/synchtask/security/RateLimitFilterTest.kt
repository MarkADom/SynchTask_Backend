package com.synchtask.security

import com.synchtask.config.RateLimitConfig
import io.github.bucket4j.Bucket
import io.mockk.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.io.PrintWriter


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitFilterTest {

    private lateinit var rateLimitConfig: RateLimitConfig
    private lateinit var filter: RateLimitFilter
    private lateinit var bucket: Bucket
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain
    private lateinit var writer: PrintWriter

    @BeforeEach
    fun setUp() {
        rateLimitConfig = mockk()
        bucket = mockk()
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        writer = mockk()
        filterChain = mockk(relaxed = true)
        filter = RateLimitFilter(rateLimitConfig)

        every { response.writer } returns writer
        every { writer.write(ofType(String::class)) } just Runs
        every { rateLimitConfig.resolveBucket(any()) } returns bucket

        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should skip filter for public endpoints`() {
        every { request.requestURI } returns "/auth/login"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        verify(exactly = 0) { response.status = any() }
    }

    @Test
    fun `should allow request when bucket allows consumption`() {
        every { request.requestURI } returns "/tasks"
        every { request.remoteAddr } returns "203.0.113.5"
        every { bucket.tryConsume(1) } returns true

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        verify(exactly = 0) { response.status = any() }
    }

    @Test
    fun `should block request when bucket is full`() {
        every { request.requestURI } returns "/tasks"
        every { request.remoteAddr } returns "203.0.113.5"
        every { bucket.tryConsume(1) } returns false

        filter.doFilter(request, response, filterChain)

        verify { response.status = 429 }
        verify { response.setHeader("Retry-After", "60") }

        val slot = slot<String>()
        verify { writer.write(capture(slot)) }
        assertEquals("Rate limit exceeded. Try again later.", slot.captured)

        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should identify user by authentication name if authenticated`() {
        val auth = UsernamePasswordAuthenticationToken("john@example.com", null, listOf())
        SecurityContextHolder.getContext().authentication = auth

        every { request.requestURI } returns "/secure"
        every { bucket.tryConsume(1) } returns true

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `should extract public IP from X-Forwarded-For if available`() {
        every { request.requestURI } returns "/secure"
        every { request.getHeader("X-Forwarded-For") } returns "203.0.113.10, 10.0.0.1"
        every { bucket.tryConsume(1) } returns true

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `should fallback to remoteAddr if all forwarded IPs are private`() {
        every { request.requestURI } returns "/secure"
        every { request.getHeader("X-Forwarded-For") } returns "10.0.0.2, 192.168.1.1"
        every { request.remoteAddr } returns "203.0.113.77"
        every { bucket.tryConsume(1) } returns true

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }
}
