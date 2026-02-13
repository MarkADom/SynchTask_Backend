package com.synchtask.activity.presentation.mapper

import com.synchtask.activity.application.dto.ActivityResponseDTO
import com.synchtask.activity.domain.entity.Activity
import com.synchtask.shared.presentation.mapper.MapperSupport.requireId

object ActivityMapper {
    fun toResponse(activity: Activity): ActivityResponseDTO =
        ActivityResponseDTO(
            id = requireId(activity.id, "Activity"),
            type = activity.type.name,
            actorEmail = activity.actor.email,
            referenceId = activity.referenceId,
            description = activity.description,
            createdAt = activity.createdAt
        )
}

