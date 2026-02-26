package com.synchtask.shared.presentation.controllers

import com.synchtask.shared.presentation.controller.TestController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Unit tests for [TestController].
 */
class TestControllerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(TestController())
            .build()
    }

    @Test
    fun `should return API is running`() {
        mockMvc.perform(get("/test/ping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("API is running"))
    }

    @Test
    fun `should clear cache`() {
        mockMvc.perform(post("/test/cache/clear"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Cache cleared"))
    }
}
