package com.synchtask.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * **Test Controller**
 *
 * Provides a simple health check endpoint to verify if the API is running.
 */
@RestController
@RequestMapping("/test")
class TestController {
    @GetMapping("/ping")
    fun ping(): ResponseEntity<String> {
        return ResponseEntity.ok("API is running")
    }
}
