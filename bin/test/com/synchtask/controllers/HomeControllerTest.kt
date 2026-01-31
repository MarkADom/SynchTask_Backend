package com.synchtask.controllers

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
        // Arrange
        val controller = HomeController()

        // Act
        val result: String = controller.homePage()

        // Assert
        assertEquals("Home", result)
    }
}
