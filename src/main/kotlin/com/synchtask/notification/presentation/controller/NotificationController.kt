package com.synchtask.notification.presentation.controller

import com.synchtask.notification.application.dto.NotificationRequestDTO
import com.synchtask.notification.application.dto.NotificationResponseDTO
import com.synchtask.notification.application.service.NotificationService
import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.shared.exception.UnauthorizedAccessException
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Notification-related HTTP endpoints.
 *
 * Delivery, permissions and persistence rules live in the service layer.
 */
@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    fun sendNotification(
        @AuthenticationPrincipal user: UserDetails,
        @RequestBody request: NotificationRequestDTO,
    ): ResponseEntity<ApiMessageResponseDTO> {
        notificationService.sendNotificationAsActor(
            actorEmail = user.username,
            recipientEmail = request.email,
            message = request.message,
            type = request.type,
            groupId = request.groupId
        )
        return ResponseEntity.ok(ApiMessageResponseDTO("Notification sent successfully"))
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getMyUnreadNotifications(@AuthenticationPrincipal user: UserDetails):
        ResponseEntity<List<NotificationResponseDTO>> {
        val notifications = notificationService.getUnreadNotifications(user.username)
        return ResponseEntity.ok(notifications)
    }


    @Deprecated("Use /notifications/me")
    @Operation(deprecated = true, summary = "Deprecated alias for /notifications/me")
    @GetMapping("/{userEmail}")
    @PreAuthorize("isAuthenticated()")
    fun getUnreadNotifications(
        @PathVariable userEmail: String,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<List<NotificationResponseDTO>> {
        if (userEmail != user.username) {
            throw UnauthorizedAccessException(
                "Deprecated endpoint only supports the authenticated principal; use /notifications/me"
            )
        }
        return getMyUnreadNotifications(user)
    }

    @PostMapping("/mark-as-read/{id}")
    @PreAuthorize("isAuthenticated()")
    fun markNotificationAsRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        notificationService.markAsRead(id, user.username)
        return ResponseEntity.ok(ApiMessageResponseDTO("Notification marked as read"))
    }

    @PostMapping("/mark-all-as-read/me")
    @PreAuthorize("isAuthenticated()")
    fun markAllMyNotificationsAsRead(@AuthenticationPrincipal user: UserDetails):
        ResponseEntity<ApiMessageResponseDTO> {
        notificationService.markAllAsRead(user.username)
        return ResponseEntity.ok(ApiMessageResponseDTO("All notifications marked as read"))
    }

    @Deprecated("Use /notifications/mark-all-as-read/me")
    @Operation(deprecated = true, summary = "Deprecated alias for /notifications/mark-all-as-read/me")
    @PostMapping("/mark-all-as-read/{userEmail}")
    @PreAuthorize("isAuthenticated()")
    fun markAllNotificationsAsRead(
        @PathVariable userEmail: String,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        if (userEmail != user.username) {
            throw UnauthorizedAccessException(
                "Deprecated endpoint only supports the authenticated principal; use /notifications/mark-all-as-read/me"
            )
        }
        return markAllMyNotificationsAsRead(user)
    }

    @DeleteMapping("/clear-cache/me", produces = ["application/json"] )
    @PreAuthorize("isAuthenticated()")
    fun clearMyNotificationCache(@AuthenticationPrincipal user: UserDetails): ResponseEntity<ApiMessageResponseDTO> {
        val deletedCount = notificationService.clearRedisCacheForUser(user.username)
        return ResponseEntity.ok(ApiMessageResponseDTO("Cleared $deletedCount notification(s) from Redis cache"))
    }

    @Deprecated("Use /notifications/clear-cache/me")
    @Operation(deprecated = true, summary = "Deprecated alias for /notifications/clear-cache/me")
    @DeleteMapping("/clear-cache/{userEmail}", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun clearNotificationCache(
        @PathVariable userEmail: String,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        if (userEmail != user.username) {
            throw UnauthorizedAccessException(
                "Deprecated endpoint only supports the authenticated principal; use /notifications/clear-cache/me"
            )
        }
        return clearMyNotificationCache(user)
    }
}
