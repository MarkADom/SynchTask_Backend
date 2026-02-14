package com.synchtask.shared.presentation.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test-cors")
class CorsTestController {
    @GetMapping
    fun testCors(): String = "CORS is working"
}
