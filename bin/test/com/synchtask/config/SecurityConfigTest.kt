package com.synchtask.config

import com.synchtask.security.JwtAuthenticationFilter
import com.synchtask.security.RateLimitFilter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.web.util.ServletRequestPathUtils
import org.springframework.web.util.UrlPathHelper
import java.time.Instant


class SecurityConfigTest {

    private val jwtAuthenticationFilter = mockk<JwtAuthenticationFilter>(relaxed = true)
    private val rateLimitFilter = mockk<RateLimitFilter>(relaxed = true)

    private val securityConfig = SecurityConfig(jwtAuthenticationFilter, rateLimitFilter)

    @Test
    fun `should create a password encoder`() {
        val encoder = securityConfig.passwordEncoder()
        val raw = "password"
        val encoded = encoder.encode(raw)
        assertTrue(encoder.matches(raw, encoded))
    }

    @Test
    fun `should create cors configuration with allowed origins`() {
        val securityConfig = SecurityConfig(mockk(relaxed = true), mockk(relaxed = true))
        val corsSource = securityConfig.corsConfigurationSource()

        val request = mockk<HttpServletRequest>()

        every { request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE) } returns null
        every { request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) } returns null
        every { request.getAttribute("jakarta.servlet.include.context_path") } returns null
        every { request.getAttribute("jakarta.servlet.include.request_uri") } returns null
        every { request.getAttribute("jakarta.servlet.include.mapping") } returns null
        every { request.requestURI } returns "/some-path"
        every { request.contextPath } returns ""
        every { request.characterEncoding } returns "UTF-8"
        every { request.httpServletMapping } returns mockk(relaxed = true)

        val config = corsSource.getCorsConfiguration(request)

        assertNotNull(config)
        assertTrue(config!!.allowedOrigins!!.contains("http://localhost:3000"))
        assertTrue(config.allowedMethods!!.contains("GET"))
        assertTrue(config.allowedHeaders!!.contains("Authorization"))
        assertTrue(config.allowCredentials!!)
    }

    @Test
    fun `should configure JWT authentication converter with roles claim`() {
        val userDetailsService = mockk<UserDetailsService>()
        val converter: JwtAuthenticationConverter = securityConfig.jwtAuthenticationConverter(userDetailsService)

        val jwt = Jwt.withTokenValue("fake-token")
            .header("alg", "none")
            .claim("sub", "test@example.com")
            .claim("roles", listOf("USER", "ADMIN"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val auth = converter.convert(jwt) as JwtAuthenticationToken

        val authorities = auth.authorities.map { it.authority }
        assertTrue(authorities.contains("ROLE_USER"))
        assertTrue(authorities.contains("ROLE_ADMIN"))
    }

    @Test
    fun `should create OAuth2 user service and return user with ROLE_USER`() {
        // Arrange
        val securityConfig = SecurityConfig(mockk(), mockk())

        val oAuth2UserRequest = mockk<OAuth2UserRequest>()
        val defaultUser = mockk<OAuth2User>()

        every { defaultUser.attributes } returns mapOf("name" to "Test User")
        every { defaultUser.getName() } returns "Test User"

        mockkConstructor(DefaultOAuth2UserService::class)
        every { anyConstructed<DefaultOAuth2UserService>().loadUser(oAuth2UserRequest) } returns defaultUser

        // Act
        val userService = securityConfig.oauth2UserService()
        val result = userService.loadUser(oAuth2UserRequest)

        // Assert
        assertNotNull(result)
        assertEquals("Test User", result.name)
        assertTrue(result.authorities.any { it.authority == "ROLE_USER" })
    }


    @Test
    fun `should return logout success handler`() {
        val handler: LogoutSuccessHandler = securityConfig.logoutSuccessHandler()
        assertNotNull(handler)
    }

    @Test
    fun `should create JWT decoder`() {
        val decoder: JwtDecoder = securityConfig.jwtDecoder()
        assertNotNull(decoder)
    }
    @Test
    fun `should create authentication manager from config`() {
        val authManager = mockk<AuthenticationManager>()
        val config = mockk<AuthenticationConfiguration>()
        every { config.authenticationManager } returns authManager

        val result = securityConfig.authenticationManager(config)
        assertEquals(authManager, result)
    }

    @Test
    fun `should build security filter chain with all custom config`() {
        val http = mockk<org.springframework.security.config.annotation.web.builders.HttpSecurity>(relaxed = true)
        val userDetailsService = mockk<UserDetailsService>()

        every { http.csrf(any()) } returns http
        every { http.cors(any()) } returns http
        every { http.sessionManagement(any()) } returns http
        every { http.authorizeHttpRequests(any()) } returns http
        every { http.oauth2Login(any()) } returns http
        every { http.oauth2ResourceServer(any()) } returns http
        every { http.logout(any()) } returns http
        every { http.exceptionHandling(any()) } returns http
        every { http.addFilterBefore(any(), any<Class<out jakarta.servlet.Filter>>()) } returns http
        every { http.build() } returns mockk<org.springframework.security.web.DefaultSecurityFilterChain>()


        val chain = securityConfig.securityFilterChain(http, userDetailsService)
        assertNotNull(chain)
    }
}
