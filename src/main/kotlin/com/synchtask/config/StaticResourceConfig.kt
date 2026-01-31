package com.synchtask.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

/**
 * Exposes uploaded files under a public URL path.
 */
@Configuration
class StaticResourceConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString()
        registry.addResourceHandler("/static/**")
            .addResourceLocations(uploadPath)
    }
}
