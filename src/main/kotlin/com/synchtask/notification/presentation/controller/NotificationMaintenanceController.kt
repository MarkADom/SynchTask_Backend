package com.synchtask.notification.presentation.controller

import com.synchtask.redis.application.service.NotificationRedisCleanupService
import com.synchtask.shared.dto.ApiMessageResponseDTO
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@Profile("dev")
@RestController
@RequestMapping("/notifications")
class NotificationMaintenanceController(
    private val redisCleanupService: NotificationRedisCleanupService,
) {
    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun triggerRedisCleanup(): ResponseEntity<ApiMessageResponseDTO> {
        redisCleanupService.cleanOldNotifications()
        return ResponseEntity.ok(ApiMessageResponseDTO("Manual Redis notification cleanup triggered."))
    }
}
