package com.synchtask.config

import com.synchtask.security.JwtTokenProvider
import com.synchtask.websocket.CustomHandshakeInterceptor
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class NotificationWebSocketConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java, NotificationWebSocketConfig::class.java)

    @Test
    fun `should load NotificationWebSocketConfig with valid beans`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(NotificationWebSocketConfig::class.java)
            assertThat(context).hasSingleBean(WebSocketProperties::class.java)
            assertThat(context).hasSingleBean(CustomHandshakeInterceptor::class.java)
        }
    }

    @Configuration
    class TestConfig {

        @Bean
        fun jwtTokenProvider(): JwtTokenProvider = mockk(relaxed = true)

        @Bean
        fun customHandshakeInterceptor(jwtTokenProvider: JwtTokenProvider): CustomHandshakeInterceptor {
            every { jwtTokenProvider.validateAndExtractUser(any()) } returns mockk(relaxed = true)
            return CustomHandshakeInterceptor(jwtTokenProvider)
        }

        @Bean
        fun webSocketProperties(): WebSocketProperties {
            return WebSocketProperties(listOf("http://localhost:3000"))
        }
    }
}
