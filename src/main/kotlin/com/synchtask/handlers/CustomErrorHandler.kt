package com.synchtask.handlers

import com.synchtask.dtos.ErrorResponseDTO
import com.synchtask.exception.FriendRequestAlreadySentException
import com.synchtask.exception.InvalidCredentialsException
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.exception.UserAlreadyExistsException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
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

    @ExceptionHandler(UserAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException): ErrorResponseDTO {
        logger.warn("⚠User already exists: ${ex.message}")
        return ErrorResponseDTO(message = "Email is already registered.", error = "Conflict")
    }

    @ExceptionHandler(UnauthorizedAccessException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    fun handleUnauthorizedAccess(ex: UnauthorizedAccessException): ErrorResponseDTO {
        logger.warn("Unauthorized access: ${ex.message}")
        return ErrorResponseDTO(message = "You do not have permission to perform this action.", error = "Forbidden")
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
        return ErrorResponseDTO(message = "Requested resource was not found.", error = "Not Found")
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneralException(ex: Exception): ErrorResponseDTO {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ErrorResponseDTO(message = "An internal server error occurred.", error = "Internal Server Error")
    }

    @ExceptionHandler(AccessDeniedException::class, AuthorizationDeniedException::class)
    fun handleAccessDenied(ex: Exception): ResponseEntity<ErrorResponseDTO> {
        logger.warn("Access denied: ${ex.message}")
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponseDTO("Access Denied", HttpStatus.FORBIDDEN.reasonPhrase))
    }

    @ExceptionHandler(FriendRequestAlreadySentException::class)
    fun handleFriendRequestAlreadySent(ex: FriendRequestAlreadySentException): ResponseEntity<ErrorResponseDTO> {
        val errorResponse = ErrorResponseDTO(
            message = ex.message ?: "Friend request already sent.",
            error = "Conflict"
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }
}
