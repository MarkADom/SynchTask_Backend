package com.synchtask.controllers

import com.synchtask.dtos.friend.FriendRequestDTO
import com.synchtask.dtos.friend.FriendResponseDTO
import com.synchtask.services.friend.FriendService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

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

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    fun sendFriendRequest(
        @RequestBody request: FriendRequestDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        friendService.sendFriendRequest(user.username, request.friendEmail)
        return ResponseEntity.ok("Friend request sent successfully!")
    }

    @PostMapping("/accept/{requestId}")
    @PreAuthorize("isAuthenticated()")
    fun acceptFriendRequest(
        @PathVariable requestId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        friendService.acceptFriendRequest(requestId, user.username)
        return ResponseEntity.ok("Friend request accepted!")
    }

    @DeleteMapping("/{friendId}")
    @PreAuthorize("isAuthenticated()")
    fun removeFriend(
        @PathVariable friendId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        friendService.removeFriend(friendId, user.username)
        return ResponseEntity.ok("Friend removed!")
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listFriends(
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<List<FriendResponseDTO>> {
        val friends = friendService.listFriends(user.username)
            .map { FriendResponseDTO.fromEntityForUser(it, user.username) }
        return ResponseEntity.ok(friends)
    }
}
