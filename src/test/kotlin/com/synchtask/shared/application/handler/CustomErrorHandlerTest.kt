package com.synchtask.shared.application.handler

import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.shared.dto.ErrorResponseDTO
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.exception.UserAlreadyExistsException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CustomErrorHandlerTest {
    private val handler = CustomErrorHandler()

    @Test
    fun `should handle ExpiredJwtException`() {
        val exception = Mockito.mock(ExpiredJwtException::class.java)
        val response: ErrorResponseDTO = handler.handleExpiredJwt(exception)

        Assertions.assertEquals("Session expired. Please log in again.", response.message)
        Assertions.assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `should handle JwtException`() {
        val exception = JwtException("Invalid token")
        val response = handler.handleJwtExceptions(exception)

        Assertions.assertEquals("Invalid authentication token.", response.message)
        Assertions.assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `should handle UserAlreadyExistsException`() {
        val exception = UserAlreadyExistsException("User already exists")
        val response = handler.handleUserAlreadyExists(exception)

        Assertions.assertEquals("Email is already registered.", response.message)
        Assertions.assertEquals("Conflict", response.error)
    }

    @Test
    fun `should handle UnauthorizedAccessException`() {
        val exception = UnauthorizedAccessException("Forbidden access")
        val response = handler.handleUnauthorizedAccess(exception)

        Assertions.assertEquals("You do not have permission to perform this action.", response.message)
        Assertions.assertEquals("Forbidden", response.error)
    }

    @Test
    fun `should handle InvalidCredentialsException`() {
        val exception = InvalidCredentialsException("Wrong login")
        val response = handler.handleInvalidCredentials(exception)

        Assertions.assertEquals("Incorrect email or password.", response.message)
        Assertions.assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `should handle ResourceNotFoundException`() {
        val exception = ResourceNotFoundException("Not found")
        val response = handler.handleResourceNotFound(exception)

        Assertions.assertEquals("Requested resource was not found.", response.message)
        Assertions.assertEquals("Not Found", response.error)
    }

    @Test
    fun `should handle general Exception`() {
        val exception = Exception("Generic error")
        val response = handler.handleGeneralException(exception)

        Assertions.assertEquals("An internal server error occurred.", response.message)
        Assertions.assertEquals("Internal Server Error", response.error)
    }
}
