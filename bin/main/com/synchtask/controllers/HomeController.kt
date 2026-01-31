package com.synchtask.controllers


import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class HomeController {

    @GetMapping
    fun homePage(): String = HOME_PAGE_RESPONSE

    companion object {
        private const val HOME_PAGE_RESPONSE = "Home"
    }
}
