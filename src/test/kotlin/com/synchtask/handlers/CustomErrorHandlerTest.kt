package com.synchtask.handlers

import com.synchtask.shared.dto.ErrorResponseDTO
import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.shared.application.handler.CustomErrorHandler
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.exception.UserAlreadyExistsException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CustomErrorHandlerTest {

    private val handler = CustomErrorHandler()

    @Test
    fun `should handle ExpiredJwtException`() {
        val exception = Mockito.mock(ExpiredJwtException::class.java)
        val response: ErrorResponseDTO = handler.handleExpiredJwt(exception)

        assertEquals("Session expired. Please log in again.", response.message)
        assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `should handle JwtException`() {
        val exception = JwtException("Invalid token")
        val response = handler.handleJwtExceptions(exception)

        assertEquals("Invalid authentication token.", response.message)
        assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `should handle UserAlreadyExistsException`() {
        val exception = UserAlreadyExistsException("User already exists")
        val response = handler.handleUserAlreadyExists(exception)

        assertEquals("Email is already registered.", response.message)
        assertEquals("Conflict", response.error)
    }

    @Test
    fun `should handle UnauthorizedAccessException`() {
        val exception = UnauthorizedAccessException("Forbidden access")
        val response = handler.handleUnauthorizedAccess(exception)

        assertEquals("You do not have permission to perform this action.", response.message)
        assertEquals("Forbidden", response.error)
    }

    @Test
    fun `should handle InvalidCredentialsException`() {
        val exception = InvalidCredentialsException("Wrong login")
        val response = handler.handleInvalidCredentials(exception)

        assertEquals("Incorrect email or password.", response.message)
        assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `should handle ResourceNotFoundException`() {
        val exception = ResourceNotFoundException("Not found")
        val response = handler.handleResourceNotFound(exception)

        assertEquals("Requested resource was not found.", response.message)
        assertEquals("Not Found", response.error)
    }

    @Test
    fun `should handle general Exception`() {
        val exception = Exception("Generic error")
        val response = handler.handleGeneralException(exception)

        assertEquals("An internal server error occurred.", response.message)
        assertEquals("Internal Server Error", response.error)
    }
}
