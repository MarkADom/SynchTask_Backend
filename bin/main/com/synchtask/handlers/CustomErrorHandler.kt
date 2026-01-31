package com.synchtask.handlers

import com.synchtask.dtos.ErrorResponseDTO
import com.synchtask.exception.InvalidCredentialsException
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.exception.UserAlreadyExistsException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * **Global Exception Handler**
 *
 * This class is responsible for handling all exceptions thrown within the application.
 * It provides centralized error handling and ensures meaningful HTTP responses.
 */
@RestControllerAdvice
class CustomErrorHandler {

    private val logger = LoggerFactory.getLogger(CustomErrorHandler::class.java)

    /**
     * Handles expired JWT tokens.
     */
    @ExceptionHandler(ExpiredJwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleExpiredJwt(ex: ExpiredJwtException): ErrorResponseDTO {
        logger.warn("JWT Token expired: ${ex.message}")
        return ErrorResponseDTO(message = "Session expired. Please log in again.", error = "Unauthorized")
    }

    /**
     * Handles invalid or malformed JWT tokens.
     */
    @ExceptionHandler(JwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleJwtExceptions(ex: JwtException): ErrorResponseDTO {
        logger.warn("Invalid JWT: ${ex.message}")
        return ErrorResponseDTO(message = "Invalid authentication token.", error = "Unauthorized")
    }

    /**
     * Handles attempts to register an already existing user.
     */
    @ExceptionHandler(UserAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException): ErrorResponseDTO {
        logger.warn("⚠User already exists: ${ex.message}")
        return ErrorResponseDTO(message = "Email is already registered.", error = "Conflict")
    }

    /**
     * Handles unauthorized access attempts.
     */
    @ExceptionHandler(UnauthorizedAccessException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    fun handleUnauthorizedAccess(ex: UnauthorizedAccessException): ErrorResponseDTO {
        logger.warn("Unauthorized access: ${ex.message}")
        return ErrorResponseDTO(message = "You do not have permission to perform this action.", error = "Forbidden")
    }

    /**
     * Handles invalid login credentials.
     */
    @ExceptionHandler(InvalidCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ErrorResponseDTO {
        logger.warn("Invalid credentials: ${ex.message}")
        return ErrorResponseDTO(message = "Incorrect email or password.", error = "Unauthorized")
    }

    /**
     * Handles not found resources (users, tasks, etc.).
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleResourceNotFound(ex: ResourceNotFoundException): ErrorResponseDTO {
        logger.warn("Resource not found: ${ex.message}")
        return ErrorResponseDTO(message = "Requested resource was not found.", error = "Not Found")
    }

    /**
     * Handles all unexpected exceptions.
     */
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneralException(ex: Exception): ErrorResponseDTO {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ErrorResponseDTO(message = "An internal server error occurred.", error = "Internal Server Error")
    }
}
