package com.synchtask.shared.presentation.controllers

import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.shared.presentation.controller.HomeController
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * **HomeControllerTest**
 *
 * Verifies that the home endpoint returns the expected static response.
 */
class HomeControllerTest {
    @Test
    fun `should return home page response`() {
        val controller = HomeController()

        val result: ApiMessageResponseDTO = controller.homePage()

        assertEquals(ApiMessageResponseDTO("Home"), result)
    }
}
