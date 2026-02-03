package com.synchtask.shared.presentation.controller


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

    @GetMapping
    fun homePage(): String = HOME_PAGE_RESPONSE

    companion object {
        private const val HOME_PAGE_RESPONSE = "Home"
    }
}
