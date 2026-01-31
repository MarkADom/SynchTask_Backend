package com.synchtask.exception


/**
 * **JWT Generation Exception**
 *
 * Exception thrown when an error occurs during JWT token creation.
 */
class JwtGenerationException (message: String, cause: Throwable? = null) : RuntimeException(message, cause)
