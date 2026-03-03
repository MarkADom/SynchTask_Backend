package com.synchtask.friend.presentation.controller

import com.synchtask.friend.application.dto.FriendRequestDTO
import com.synchtask.friend.application.dto.FriendResponseDTO
import com.synchtask.friend.application.service.FriendService
import com.synchtask.shared.dto.ApiMessageResponseDTO
import jakarta.validation.Valid
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
 * Friend-related HTTP endpoints.
 *
 * Friendship rules and validations are enforced at service level.
 */
@RestController
@RequestMapping("/friends")
class FriendController(
    private val friendService: FriendService,
) {
    @PostMapping("/request", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun sendFriendRequest(
        @Valid @RequestBody request: FriendRequestDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        friendService.sendFriendRequest(user.username, request.friendEmail)
        return ResponseEntity.ok(ApiMessageResponseDTO("Friend request sent successfully!"))
    }

    @PostMapping("/accept/{requestId}", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun acceptFriendRequest(
        @PathVariable requestId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        friendService.acceptFriendRequest(requestId, user.username)
        return ResponseEntity.ok(ApiMessageResponseDTO("Friend request accepted!"))
    }

    @DeleteMapping("/{friendId}", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun removeFriend(
        @PathVariable friendId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<ApiMessageResponseDTO> {
        friendService.removeFriend(friendId, user.username)
        return ResponseEntity.ok(ApiMessageResponseDTO("Friend removed!"))
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listFriends(@AuthenticationPrincipal user: UserDetails):
        ResponseEntity<List<FriendResponseDTO>> {
        val friends = friendService.listFriends(user.username)
        return ResponseEntity.ok(friends)
    }
}
