package com.synchtask.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
open class UnauthorizedAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
