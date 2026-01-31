package com.synchtask.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

/**
 * Jackson setup shared across the application.
 */
@Configuration
class JacksonConfig(
    private val objectMapper: ObjectMapper
) {

    @PostConstruct
    fun registerModules() {
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerModule(
            KotlinModule.Builder().build()
        )
    }
}
