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
 * **Friend Controller**
 *
 * Manages friendship-related operations such as sending requests,
 * accepting requests, removing friends, and listing friends.
 */
@RestController
@RequestMapping("/friends")
class FriendController(
    private val friendService: FriendService
) {

    /**
     * Sends a friend request to another user.
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    fun sendFriendRequest(
        @RequestBody request: FriendRequestDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        friendService.sendFriendRequest(user.username, request.friendEmail)
        return ResponseEntity.ok("Friend request sent successfully!")
    }

    /**
     * Accepts a pending friend request.
     */
    @PostMapping("/accept/{requestId}")
    @PreAuthorize("isAuthenticated()")
    fun acceptFriendRequest(
        @PathVariable requestId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        friendService.acceptFriendRequest(requestId, user.username)
        return ResponseEntity.ok("Friend request accepted!")
    }

    /**
     * Removes an existing friend.
     */
    @DeleteMapping("/{friendId}")
    @PreAuthorize("isAuthenticated()")
    fun removeFriend(
        @PathVariable friendId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<String> {
        friendService.removeFriend(friendId, user.username)
        return ResponseEntity.ok("Friend removed!")
    }

    /**
     * Retrieves a list of the authenticated user's friends.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listFriends(
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<FriendResponseDTO>> {
        val friends = friendService.listFriends(user.username)
            .map { FriendResponseDTO.fromEntity(it) }
        return ResponseEntity.ok(friends)
    }
}
