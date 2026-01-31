package com.synchtask.controllers

import com.synchtask.dtos.notification.NotificationRequestDTO
import com.synchtask.dtos.notification.NotificationResponseDTO
import com.synchtask.services.notification.NotificationRedisCleanupService
import com.synchtask.services.notification.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * **Notification Controller**
 *
 * Handles all notification-related API requests.
 */
@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val redisCleanupService: NotificationRedisCleanupService
) {

    /**
     * Sends a notification to a user or group.
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_COLLABORATOR')")
    fun sendNotification(@RequestBody request: NotificationRequestDTO): ResponseEntity<String> {
        notificationService.sendNotification(request.email, request.message, request.type, request.groupId)
        return ResponseEntity.ok("Notification sent successfully")
    }

    /**
     * Fetches unread notifications for a user.
     * Admins can access others' notifications.
     */
    @GetMapping("/{userEmail}")
    @PreAuthorize("#userEmail == authentication.name or hasAuthority('ROLE_ADMIN')")
    fun getUnreadNotifications(@PathVariable userEmail: String): ResponseEntity<List<NotificationResponseDTO>> {
        val notifications = notificationService.getUnreadNotifications(userEmail)
        return ResponseEntity.ok(notifications)
    }

    /**
     * Marks a specific notification as read.
     * Only accessible by the authenticated user.
     */
    @PostMapping("/mark-as-read/{id}")
    @PreAuthorize("isAuthenticated()")
    fun markNotificationAsRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok("Notification marked as read")
    }

    /**
     * Marks all notifications as read for a user.
     */
    @PostMapping("/mark-all-as-read/{userEmail}")
    @PreAuthorize("#userEmail == authentication.name or hasAuthority('ROLE_ADMIN')")
    fun markAllNotificationsAsRead(@PathVariable userEmail: String): ResponseEntity<String> {
        notificationService.markAllAsRead(userEmail)
        return ResponseEntity.ok("All notifications marked as read")
    }

    /**
     * Clears the Redis cache for a user's notifications.
     */
    @DeleteMapping("/clear-cache/{userEmail}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #userEmail == authentication.name")
    fun clearNotificationCache(@PathVariable userEmail: String): ResponseEntity<String> {
        val deletedCount = notificationService.clearRedisCacheForUser(userEmail)
        return ResponseEntity.ok("Cleared $deletedCount notification(s) from Redis cache for $userEmail")
    }

    /**
     * Triggers a Redis cleanup process manually.
     * Only accessible by ADMIN users.
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun triggerRedisCleanup(): ResponseEntity<String> {
        redisCleanupService.cleanOldNotifications()
        return ResponseEntity.ok("Manual Redis notification cleanup triggered.")
    }
}
