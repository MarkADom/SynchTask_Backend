package com.synchtask.shared.presentation.controller

import com.synchtask.shared.dto.ApiMessageResponseDTO
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lightweight health check endpoint.
 */
@Hidden
@Profile("dev")
@RestController
@RequestMapping("/test")
class TestController {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<ApiMessageResponseDTO> {
        return ResponseEntity.ok(ApiMessageResponseDTO("API is running"))
    }

    @PostMapping("/cache/clear")
    fun clearCache(): ResponseEntity<ApiMessageResponseDTO> {
        return ResponseEntity.ok(ApiMessageResponseDTO("Cache cleared"))
    }
}
