package com.synchtask.task.domain.exception

import com.synchtask.task.domain.entity.TaskStatus

class InvalidTaskStatusTransitionException(
    from: TaskStatus,
    to: TaskStatus
) : RuntimeException(
    "Invalid task status transition from $from to $to"
)
