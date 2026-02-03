package com.synchtask.shared.presentation.controllers

import com.synchtask.shared.presentation.controller.TestController
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Unit test for [TestController].
 */
@ExtendWith(MockKExtension::class)
class TestControllerTest {

    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(TestController()).build()

    @Test
    fun `should return API is running`() {
        mockMvc.perform(get("/test/ping"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string("API is running"))
    }
}
