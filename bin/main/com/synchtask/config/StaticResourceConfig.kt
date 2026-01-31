package com.synchtask.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

/**
 * **Static Resource Configuration**
 *
 * Exposes uploaded files to public URL endpoints.
 */
@Configuration
class StaticResourceConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString()
        registry.addResourceHandler("/static/**")
            .addResourceLocations(uploadPath)
    }
}
