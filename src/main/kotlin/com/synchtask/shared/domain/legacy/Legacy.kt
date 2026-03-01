package com.synchtask.shared.domain.legacy

/**
 * Marks code paths kept temporarily for backward compatibility.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Legacy
