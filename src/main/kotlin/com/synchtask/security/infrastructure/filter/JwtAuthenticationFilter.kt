package com.synchtask.security.infrastructure.filter

import com.synchtask.security.infrastructure.jwt.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Extracts and validates JWTs from incoming requests
 * and populates the SecurityContext when valid.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        log.debug("Processing request: ${request.method} $path")

        if (isPublicEndpoint(path)) {
            log.debug("Skipping JWT authentication for: $path")
            filterChain.doFilter(request, response)
            return
        }

        val token = jwtTokenProvider.extractTokenFromRequest(request)

        if (token.isNullOrBlank()) {
            log.warn("No JWT token found for: ${request.method} $path")
            filterChain.doFilter(request, response)
            return
        }

        val userDetails: UserDetails? = jwtTokenProvider.validateAndExtractUser(token)

        if (userDetails == null) {
            log.warn("Invalid JWT token for: ${request.method} $path")
            filterChain.doFilter(request, response)
            return
        }

        val authentication =
            UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
        SecurityContextHolder.getContext().authentication = authentication
        log.info("User authenticated: ${userDetails.username}")

        filterChain.doFilter(request, response)
    }

    private fun isPublicEndpoint(path: String): Boolean {
        return SWAGGER_ENDPOINTS.any { path.startsWith(it) } ||
            PUBLIC_ENDPOINTS.any { path.startsWith(it) }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

        private val PUBLIC_ENDPOINTS =
            listOf(
                "/auth/login",
                "/auth/register",
                "/oauth2/"
            )

        private val SWAGGER_ENDPOINTS =
            listOf(
                "/swagger-ui",
                "/v3/api-docs",
                "/swagger-resources",
                "/webjars"
            )
    }
}
