package com.synchtask.exception

/**
 * **Custom Exception for JWT Validation**
 *
 * Centralized exception handling for JWT errors.
 */
class JwtValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


