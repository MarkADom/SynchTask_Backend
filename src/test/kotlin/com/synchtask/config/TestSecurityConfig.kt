package com.synchtask.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler

@TestConfiguration
class TestSecurityConfig {
    @Bean
    fun testLogoutHandler(): SecurityContextLogoutHandler {
        return SecurityContextLogoutHandler() // or spyk(SecurityContextLogoutHandler()) if you want to check calls
    }
}
