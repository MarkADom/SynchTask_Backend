package com.synchtask.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
open class UserAlreadyExistsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
