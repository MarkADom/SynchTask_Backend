package com.synchtask.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SwaggerConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(SwaggerConfig::class.java)

    @Test
    fun `should load GroupedOpenApi bean from SwaggerConfig`() {
        contextRunner.run { context ->
            val bean = context.getBean(GroupedOpenApi::class.java)
            assertThat(bean).isNotNull
            assertThat(bean.group).isEqualTo("synchtask")
        }
    }
}
