package com.synchtask.activity.domain.model

/**
 * Represents the type of activity that occurred in the system.
 *
 * This enum is part of the domain language.
 */
enum class ActivityType {

    // Task related
    TASK_CREATED,
    TASK_UPDATED,
    TASK_STATUS_CHANGED,
    TASK_ASSIGNED,
    TASK_COMMENTED,

    // Board related (future)
    BOARD_CREATED,
    BOARD_UPDATED,

    // Project related (future)
    PROJECT_CREATED,
    PROJECT_UPDATED
}
