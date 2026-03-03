package com.synchtask.activity.presentation.controller

import com.synchtask.activity.application.dto.ActivityResponseDTO
import com.synchtask.activity.application.service.ActivityService
import com.synchtask.activity.presentation.mapper.ActivityMapper
import com.synchtask.user.application.service.AuthenticatedUserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/activities")
class ActivityController(
    private val activityService: ActivityService,
    private val authenticatedUserService: AuthenticatedUserService
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getMyActivities(@AuthenticationPrincipal user: UserDetails): List<ActivityResponseDTO> {
        val userEntity = authenticatedUserService.requireUser(user)

        return activityService
            .getActivitiesForUser(userEntity)
            .map { ActivityMapper.toResponse(it) }
    }
}
