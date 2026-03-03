package com.synchtask.shared.application.handler

import com.synchtask.friend.domain.exception.FriendRequestAlreadySentException
import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.shared.dto.ErrorResponseDTO
import com.synchtask.shared.exception.AccessDeniedException
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.exception.UserAlreadyExistsException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus


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
    fun `should handle UserAlreadyExistsException as conflict`() {
        val exception = UserAlreadyExistsException("User already exists")
        val response = handler.handleDomainConflict(exception)

        Assertions.assertEquals("User already exists", response.message)
        Assertions.assertEquals("Conflict", response.error)
    }

    @Test
    fun `should handle UnauthorizedAccessException as forbidden`() {
        val exception = UnauthorizedAccessException("Forbidden access")
        val response = handler.handleDomainAccessDenied(exception)

        Assertions.assertEquals("Forbidden access", response.message)
        Assertions.assertEquals("Forbidden", response.error)
    }

    @Test
    fun `should handle AccessDeniedException as forbidden`() {
        val exception = AccessDeniedException("Role is insufficient")
        val response = handler.handleDomainAccessDenied(exception)

        Assertions.assertEquals("Role is insufficient", response.message)
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

        Assertions.assertEquals("Not found", response.message)
        Assertions.assertEquals("Not Found", response.error)
    }

    @Test
    fun `should handle general Exception`() {
        val exception = Exception("Generic error")
        val response = handler.handleGeneralException(exception)

        Assertions.assertEquals("An internal server error occurred.", response.message)
        Assertions.assertEquals("Internal Server Error", response.error)
    }

    @Test
    fun `should handle spring access denied exceptions`() {
        val response = handler.handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException("denied"))

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        Assertions.assertEquals("Access Denied", response.body?.message)
        Assertions.assertEquals("Forbidden", response.body?.error)
    }

    @Test
    fun `should handle friend request already sent as conflict`() {
        val response = handler.handleDomainConflict(FriendRequestAlreadySentException("Already sent"))

        Assertions.assertEquals("Already sent", response.message)
        Assertions.assertEquals("Conflict", response.error)
    }
}
