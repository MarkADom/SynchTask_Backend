package com.synchtask.config

import com.synchtask.shared.presentation.controllers.CorsTestController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

class CorsConfigTest {

    private fun mockMvcWithCors(allowedOrigins: List<String>): MockMvc {
        val corsConfig = CorsConfiguration().apply {
            allowedMethods = listOf("GET", "POST", "OPTIONS")
            this.allowedOrigins = allowedOrigins
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/test-cors", corsConfig)
        }

        val corsFilter = CorsFilter(source)

        val builder = MockMvcBuilders.standaloneSetup(CorsTestController())
        val filtered = builder.addFilters(corsFilter) as StandaloneMockMvcBuilder
        return filtered.build()

    }


    @Test
    fun `should allow CORS for allowed origin`() {
        val mockMvc = mockMvcWithCors(listOf("http://localhost:3000"))

        val response = mockMvc.perform(
            MockMvcRequestBuilders.options("/test-cors")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
        ).andReturn().response

        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `should reject CORS for disallowed origin`() {
        val mockMvc = mockMvcWithCors(listOf("http://localhost:3000"))

        val response = mockMvc.perform(
            MockMvcRequestBuilders.options("/test-cors")
                .header(HttpHeaders.ORIGIN, "http://evil.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
        ).andReturn().response

        assertThat(response.status).isEqualTo(403)
    }
}

