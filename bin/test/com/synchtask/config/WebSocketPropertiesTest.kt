package com.synchtask.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class WebSocketPropertiesTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)
        .withPropertyValues(
            "websocket.enabled=true",
            "websocket.allowed-origins[0]=http://localhost:3000",
            "websocket.allowed-origins[1]=https://app.synchtask.dev"
        )

    @Test
    fun `should bind allowedOrigins correctly from properties`() {
        contextRunner.run { context ->
            val props = context.getBean(WebSocketProperties::class.java)
            assertThat(props.allowedOrigins).containsExactly(
                "http://localhost:3000",
                "https://app.synchtask.dev"
            )
        }
    }

    @EnableConfigurationProperties(WebSocketProperties::class)
    class TestConfig
}
