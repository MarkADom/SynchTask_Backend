package com.synchtask.shared.presentation.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lightweight health check endpoint.
 */
@RestController
@RequestMapping("/test")
class TestController {
    @GetMapping("/ping")
    fun ping(): ResponseEntity<String> {
        return ResponseEntity.ok("API is running")
    }
}
