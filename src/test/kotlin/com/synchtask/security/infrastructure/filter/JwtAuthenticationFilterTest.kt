package com.synchtask.security.infrastructure.filter

import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import io.mockk.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import kotlin.test.assertEquals
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtAuthenticationFilterTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var jwtAuthenticationFilter: TestableJwtAuthenticationFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = mockk()
        jwtAuthenticationFilter = TestableJwtAuthenticationFilter(jwtTokenProvider)
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        filterChain = mockk(relaxed = true)
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should skip authentication for public endpoint`() {
        every { request.requestURI } returns "/auth/login"
        every { request.method } returns "POST"

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `should continue filter if token is missing`() {
        every { request.requestURI } returns "/secure/tasks"
        every { request.method } returns "GET"
        every { jwtTokenProvider.extractTokenFromRequest(request) } returns null

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `should continue filter if token is invalid`() {
        every { request.requestURI } returns "/secure/tasks"
        every { request.method } returns "GET"
        every { jwtTokenProvider.extractTokenFromRequest(request) } returns "invalid.token"
        every { jwtTokenProvider.validateAndExtractUser("invalid.token") } returns null

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `should authenticate and set SecurityContext if token is valid`() {
        val token = "valid.token"
        val userDetails = FakeUserDetails("john.doe")

        every { request.requestURI } returns "/secure/tasks"
        every { request.method } returns "GET"
        every { jwtTokenProvider.extractTokenFromRequest(request) } returns token
        every { jwtTokenProvider.validateAndExtractUser(token) } returns userDetails

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals("john.doe", auth?.name)
        assertEquals(userDetails, auth?.principal)
        assertEquals(emptyList(), auth?.authorities)
        verify { filterChain.doFilter(request, response) }
    }

    // Subclass to expose protected method for unit testing
    class TestableJwtAuthenticationFilter(
        jwtTokenProvider: JwtTokenProvider
    ) : JwtAuthenticationFilter(jwtTokenProvider) {
        public override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            super.doFilterInternal(request, response, filterChain)
        }
    }

    // Realistic implementation of UserDetails for isolated test
    class FakeUserDetails(private val username: String) : UserDetails {
        override fun getUsername() = username

        override fun getAuthorities() = emptyList<GrantedAuthority>()

        override fun getPassword() = null

        override fun isAccountNonExpired() = true

        override fun isAccountNonLocked() = true

        override fun isCredentialsNonExpired() = true

        override fun isEnabled() = true
    }
}
