package com.synchtask.controllers

import com.synchtask.dtos.notification.NotificationRequestDTO
import com.synchtask.dtos.notification.NotificationResponseDTO
import com.synchtask.services.redis.NotificationRedisCleanupService
import com.synchtask.services.notification.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * Notification-related HTTP endpoints.
 *
 * Delivery, permissions and persistence rules live in the service layer.
 */
@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val redisCleanupService: NotificationRedisCleanupService
) {

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_COLLABORATOR')")
    fun sendNotification(@RequestBody request: NotificationRequestDTO): ResponseEntity<String> {
        notificationService.sendNotification(request.email, request.message, request.type, request.groupId)
        return ResponseEntity.ok("Notification sent successfully")
    }

    @GetMapping("/{userEmail}")
    @PreAuthorize("#userEmail == authentication.name or hasAuthority('ROLE_ADMIN')")
    fun getUnreadNotifications(@PathVariable userEmail: String): ResponseEntity<List<NotificationResponseDTO>> {
        val notifications = notificationService.getUnreadNotifications(userEmail)
        return ResponseEntity.ok(notifications)
    }

    @PostMapping("/mark-as-read/{id}")
    @PreAuthorize("isAuthenticated()")
    fun markNotificationAsRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok("Notification marked as read")
    }

    @PostMapping("/mark-all-as-read/{userEmail}")
    @PreAuthorize("#userEmail == authentication.name or hasAuthority('ROLE_ADMIN')")
    fun markAllNotificationsAsRead(@PathVariable userEmail: String): ResponseEntity<String> {
        notificationService.markAllAsRead(userEmail)
        return ResponseEntity.ok("All notifications marked as read")
    }

    @DeleteMapping("/clear-cache/{userEmail}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #userEmail == authentication.name")
    fun clearNotificationCache(@PathVariable userEmail: String): ResponseEntity<String> {
        val deletedCount = notificationService.clearRedisCacheForUser(userEmail)
        return ResponseEntity.ok("Cleared $deletedCount notification(s) from Redis cache for $userEmail")
    }

    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun triggerRedisCleanup(): ResponseEntity<String> {
        redisCleanupService.cleanOldNotifications()
        return ResponseEntity.ok("Manual Redis notification cleanup triggered.")
    }
}
