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
    TASK_ATTACHMENT_ADDED,
    TASK_ATTACHMENT_REMOVED,

    // Board related
    BOARD_CREATED,
    BOARD_UPDATED,
    BOARD_DELETED,
    BOARD_COLLABORATORS_UPDATED,

    // Project related
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,
    PROJECT_BOARDS_UPDATED
}