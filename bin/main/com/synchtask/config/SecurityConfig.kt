package com.synchtask.config

import com.synchtask.security.CustomJwtAuthenticationConverter
import com.synchtask.security.JwtAuthenticationFilter
import com.synchtask.security.RateLimitFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment

import java.time.Duration

/**
 * **Security Configuration**
 *
 * - Configures **JWT**, **OAuth2**, **Rate Limiting**, and **CORS**.
 * - Implements **Role-Based Access Control (RBAC)** for secure API access.
 * - Ensures **WebSocket authentication** with **OAuth2 support**.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    private val env: Environment
) {

    /**
     * **Password Encoder**
     *
     * - Uses **BCrypt** for secure password hashing.
     * - Ensures compatibility with stored passwords in the database.
     */
    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    /**
     * **Authentication Manager**
     *
     * - Provides authentication handling for Spring Security.
     * - Supports **JWT-based authentication**.
     */
    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    /**
     * **Security Filter Chain**
     *
     * - Configures **JWT authentication**, **OAuth2 login**, and **access control**.
     * - Implements **stateless session management** (for REST APIs).
     * - Protects APIs with **Role-Based Access Control (RBAC)**.
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity, userDetailsService: UserDetailsService): SecurityFilterChain {
        return http
            .csrf { it.disable() } // CSRF disabled (Stateless REST API)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth

                    // Allow CORS preflight requests for the test endpoint
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Allow Public Access to Swagger & API Docs
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico",
                        // WebSocket Endpoints (Allow Handshake)
                        "/ws",
                        "/ws-notifications",
                        "/ws/**",
                        "/ws-notifications/**",
                    ).permitAll()

                    // Public Endpoints (Accessible Without Authentication)
                    .requestMatchers(
                        "/actuator/**",
                        "/auth/.well-known/openid-configuration",
                        "/auth/.well-known/oauth-authorization-server",
                        "/jwks",
                        "/auth/**",
                        "/auth/register",
                        "/auth/login",
                        "/auth/logout",
                        "/error"
                    ).permitAll()

                    // OAuth2 Endpoints
                    .requestMatchers("/oauth2/**").permitAll()

                    // Protected Endpoints (Require Authentication)
                    .requestMatchers("/notifications/**").authenticated()

                    // All other requests require authentication
                    .anyRequest().authenticated()
            }
            // OAuth2 Login Handling
            .oauth2Login { oauth2 ->
                oauth2.successHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_OK
                    response.writer.write("{\"message\": \"OAuth2 Login Successful\"}")
                }
            }
            // JWT Resource Server (Validates JWT Tokens)
            .oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(userDetailsService))
                }
            }
            // Logout Configuration
            .logout { logout ->
                logout.logoutUrl("/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .logoutSuccessHandler(logoutSuccessHandler())
            }
            // Exception Handling
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                }
            }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }


            // Security Filters (Rate Limiting & JWT)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, BearerTokenAuthenticationFilter::class.java)
            .build()
    }

    /**
     * **JWT Decoder Bean**
     *
     * - Uses Nimbus decoder to fetch keys from the JWKS endpoint.
     * - Applies standard and custom JWT validations.
     * - Adds a 5-minute clock skew to handle time drift between services.
     *
     * @return Configured JwtDecoder bean.
     */
    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder
            .withJwkSetUri("http://localhost:8081/jwks")
            .build()

        // Clock skew tolerance of 5 minutes
        val timestampValidator = JwtTimestampValidator(Duration.ofMinutes(5))

        // Standard JWT validations + custom timestamp validator
        val defaultValidator = JwtValidators.createDefault()
        val compositeValidator = DelegatingOAuth2TokenValidator(timestampValidator, defaultValidator)

        decoder.setJwtValidator(compositeValidator)

        return decoder
    }

    /**
     * **JWT Authentication Converter**
     *
     * - Extracts user roles from JWT claims.
     * - Ensures **RBAC (Role-Based Access Control)** works correctly.
     */
    @Bean

    fun jwtAuthenticationConverter(userDetailsService: UserDetailsService): Converter<Jwt, out AbstractAuthenticationToken> {
        return CustomJwtAuthenticationConverter(userDetailsService)
    }

    /**
     * **OAuth2 User Service**
     *
     * - Fetches user details after successful OAuth2 authentication.
     * - Assigns default role `ROLE_USER` for new OAuth2 logins.
     */
    @Bean
    fun oauth2UserService(): OAuth2UserService<OAuth2UserRequest, OAuth2User> {
        return OAuth2UserService { userRequest ->
            val delegate = DefaultOAuth2UserService()
            val user = delegate.loadUser(userRequest)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

            object : OAuth2User {
                override fun getAuthorities() = authorities
                override fun getAttributes() = user.attributes
                override fun getName() = user.attributes["name"]?.toString() ?: "Unknown"
            }
        }
    }

    /**
     * **Logout Success Handler**
     *
     * - Handles logout responses.
     * - Ensures a clean logout experience.
     */
    @Bean
    fun logoutSuccessHandler() = LogoutSuccessHandler { _, response, _ ->
        response.status = HttpServletResponse.SC_OK
        response.writer.write("{\"message\": \"Logout successful\"}")
    }
}
