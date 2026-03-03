package com.synchtask.shared.application.handler

import com.synchtask.friend.domain.exception.FriendRequestAlreadySentException
import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.shared.dto.ErrorResponseDTO
import com.synchtask.shared.exception.AccessDeniedException
import com.synchtask.shared.exception.DomainConflictException
import com.synchtask.shared.exception.InvalidInputException
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.domain.exception.UserAlreadyExistsException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Centralizes HTTP error responses for the API.
 */
@RestControllerAdvice
class CustomErrorHandler {
    private val logger = LoggerFactory.getLogger(CustomErrorHandler::class.java)

    @ExceptionHandler(ExpiredJwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleExpiredJwt(ex: ExpiredJwtException): ErrorResponseDTO {
        logger.warn("JWT Token expired: ${ex.message}")
        return ErrorResponseDTO(message = "Session expired. Please log in again.", error = "Unauthorized")
    }

    @ExceptionHandler(JwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleJwtExceptions(ex: JwtException): ErrorResponseDTO {
        logger.warn("Invalid JWT: ${ex.message}")
        return ErrorResponseDTO(message = "Invalid authentication token.", error = "Unauthorized")
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ErrorResponseDTO {
        logger.warn("Invalid credentials: ${ex.message}")
        return ErrorResponseDTO(message = "Incorrect email or password.", error = "Unauthorized")
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleResourceNotFound(ex: ResourceNotFoundException): ErrorResponseDTO {
        logger.warn("Resource not found: ${ex.message}")
        return ErrorResponseDTO(message = ex.message ?: "Requested resource was not found.", error = "Not Found")
    }

    @ExceptionHandler(AccessDeniedException::class, UnauthorizedAccessException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    fun handleDomainAccessDenied(ex: RuntimeException): ErrorResponseDTO {
        logger.warn("Access denied: ${ex.message}")
        return ErrorResponseDTO(message = ex.message ?: "Access denied", error = "Forbidden")
    }

    @ExceptionHandler(
        DomainConflictException::class,
        UserAlreadyExistsException::class,
        FriendRequestAlreadySentException::class
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun handleDomainConflict(ex: RuntimeException): ErrorResponseDTO {
        logger.warn("Domain conflict: ${ex.message}")
        return ErrorResponseDTO(message = ex.message ?: "Domain conflict", error = "Conflict")
    }

    @ExceptionHandler(InvalidInputException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleInvalidInput(ex: InvalidInputException): ErrorResponseDTO {
        logger.warn("Invalid input: ${ex.message}")
        return ErrorResponseDTO(message = ex.message ?: "Invalid input", error = "Bad Request")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ErrorResponseDTO {
        val message = ex.bindingResult.allErrors.joinToString(", ") { error ->
            when (error) {
                is FieldError -> "${error.field}: ${error.defaultMessage}"
                else -> error.defaultMessage ?: "Invalid request"
            }
        }
        logger.warn("Validation failed: $message")
        return ErrorResponseDTO(message = message.ifBlank { "Validation failed" }, error = "Bad Request")
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleConstraintViolation(ex: ConstraintViolationException): ErrorResponseDTO {
        val message = ex.constraintViolations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
        logger.warn("Constraint violation: $message")
        return ErrorResponseDTO(message = message.ifBlank { "Constraint violation" }, error = "Bad Request")
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleIllegalArgument(ex: IllegalArgumentException): ErrorResponseDTO {
        logger.warn("Illegal argument: ${ex.message}")
        return ErrorResponseDTO(message = ex.message ?: "Invalid request", error = "Bad Request")
    }

    @ExceptionHandler(SpringAccessDeniedException::class, AuthorizationDeniedException::class)
    fun handleSpringAccessDenied(ex: Exception): ResponseEntity<ErrorResponseDTO> {
        logger.warn("Access denied by spring security: ${ex.message}")

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponseDTO("Access Denied", HttpStatus.FORBIDDEN.reasonPhrase))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneralException(ex: Exception): ErrorResponseDTO {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ErrorResponseDTO(message = "An internal server error occurred.", error = "Internal Server Error")
    }
}
