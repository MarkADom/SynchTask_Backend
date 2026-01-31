package com.synchtask.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

/**
 * Configures the global ObjectMapper to support Java 8 Date/Time types (e.g., LocalDateTime)
 * and Kotlin-specific behavior (like default parameters and null-safety).
 */
@Configuration
class JacksonConfig(
    private val objectMapper: ObjectMapper
) {

    /**
     * Registers required modules to the default ObjectMapper instance.
     */
    @PostConstruct
    fun registerModules() {
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerModule(
            KotlinModule.Builder().build()
        )
    }
}
