package com.synchtask.activity.application.event

import com.synchtask.activity.domain.entity.Activity

data class ActivityRecordedEvent(
    val activity: Activity,
    val contextSnapshot: ActivityContextSnapshot? = null
)
