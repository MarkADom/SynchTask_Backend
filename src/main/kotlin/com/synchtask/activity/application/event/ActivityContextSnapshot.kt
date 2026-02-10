package com.synchtask.activity.application.event

/**
 * Optional immutable context snapshot used by downstream consumers
 * (mainly notifications) when reference entities may no longer exist,
 * such as DELETE events.
 */
data class ActivityContextSnapshot(
    val ownerEmail: String? = null,
    val memberEmails: Set<String> = emptySet(),
    val collaboratorEmails: Set<String> = emptySet()
)
