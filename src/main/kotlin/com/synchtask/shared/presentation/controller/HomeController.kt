package com.synchtask.shared.presentation.controller

import com.synchtask.shared.dto.ApiMessageResponseDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Minimal root endpoint.
 *
 * Used as a health/check entry point and default landing response.
 */
@RestController
@RequestMapping("/")
class HomeController {
    @GetMapping(produces = ["application/json"])
    fun homePage(): ApiMessageResponseDTO = ApiMessageResponseDTO("Home")
}
