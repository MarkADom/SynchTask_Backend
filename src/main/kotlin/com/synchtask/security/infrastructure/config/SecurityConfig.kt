package com.synchtask.security.infrastructure.config

import com.synchtask.security.infrastructure.filter.JwtAuthenticationFilter
import com.synchtask.security.infrastructure.filter.RateLimitFilter
import com.synchtask.security.infrastructure.jwt.CustomJwtAuthenticationConverter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
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
import java.time.Duration

/**
 * Central security configuration for HTTP APIs and WebSocket access.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
) {
    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

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
                        // TODO(dev-security): restrict actuator exposure outside dev/demo | keep public now for local diagnostics
                        "/actuator/**",
                        "/auth/.well-known/openid-configuration",
                        "/auth/.well-known/oauth-authorization-server",
                        "/jwks",
                        "/auth/register",
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/jwks",
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

    @Bean
    fun jwtDecoder(): JwtDecoder {
        // TODO(config-security): externalize JWK set URI to env/property | localhost default kept for local portfolio setup
        val decoder =
            NimbusJwtDecoder
                .withJwkSetUri("http://localhost:8081/jwks")
                .build()

        val timestampValidator = JwtTimestampValidator(Duration.ofMinutes(5))

        // Standard JWT validations + custom timestamp validator
        val defaultValidator = JwtValidators.createDefault()
        val compositeValidator = DelegatingOAuth2TokenValidator(timestampValidator, defaultValidator)

        decoder.setJwtValidator(compositeValidator)

        return decoder
    }

    @Bean
    fun jwtAuthenticationConverter(
        userDetailsService: UserDetailsService
    ): Converter<Jwt, out AbstractAuthenticationToken> {
        return CustomJwtAuthenticationConverter(userDetailsService)
    }

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

    @Bean
    fun logoutSuccessHandler() = LogoutSuccessHandler { _, response, _ ->
        response.status = HttpServletResponse.SC_OK
        response.writer.write("{\"message\": \"Logout successful\"}")
    }
}
