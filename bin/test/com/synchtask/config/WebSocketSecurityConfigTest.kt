package com.synchtask.config

import com.synchtask.security.JwtTokenProvider
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.config.SimpleBrokerRegistration

class WebSocketSecurityConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestWebSocketSecurityBeans::class.java)

    @Test
    fun `should configure message broker correctly`() {
        contextRunner.run { context ->
            val config = context.getBean(WebSocketSecurityConfig::class.java)
            val broker = TestMessageBrokerRegistry()

            config.configureMessageBroker(broker)

            assertThat(broker.applicationPrefixes).containsExactly("/app")
            assertThat(broker.userPrefix).isEqualTo("/user")
            assertThat(broker.simpleBrokerEnabled).containsExactly("/topic", "/queue")
        }
    }

    @Configuration
    class TestWebSocketSecurityBeans {

        @Bean
        fun jwtTokenProvider(): JwtTokenProvider = mockk(relaxed = true)

        @Bean
        fun env(): Environment = mockk(relaxed = true)

        @Bean
        fun config(): WebSocketSecurityConfig =
            WebSocketSecurityConfig(jwtTokenProvider(), env())
    }

    class TestMessageBrokerRegistry :
        MessageBrokerRegistry(mockk(relaxed = true), mockk(relaxed = true)) {

        val applicationPrefixes = mutableListOf<String>()
        var userPrefix: String = ""
        var simpleBrokerEnabled: List<String> = emptyList()

        override fun setApplicationDestinationPrefixes(vararg prefixes: String?): MessageBrokerRegistry {
            applicationPrefixes.addAll(prefixes.filterNotNull())
            return this
        }

        override fun setUserDestinationPrefix(prefix: String): MessageBrokerRegistry {
            userPrefix = prefix
            return this
        }


        override fun enableSimpleBroker(vararg destinationPrefixes: String?): SimpleBrokerRegistration {
            simpleBrokerEnabled = destinationPrefixes.filterNotNull()
            return mockk(relaxed = true)
        }
    }

}
