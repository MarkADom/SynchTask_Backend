package com.synchtask

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Base class for integration tests.
 *
 * - Loads full Spring context.
 * - Uses the "test" profile.
 * - Should be extended only by tests that require real infrastructure.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseIntegrationTest
