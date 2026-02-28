package com.synchtask.chat.presentation.controller

import com.synchtask.chat.application.dto.ChatMessageDTO
import com.synchtask.chat.application.dto.ChatRoomDTO
import com.synchtask.chat.application.service.ChatService
import com.synchtask.chat.application.service.KeyExchangeService
import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.shared.exception.UnauthorizedAccessException
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Chat-related HTTP endpoints.
 *
 * Message delivery and access checks are handled at service level.
 */
@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
    private val keyExchangeService: KeyExchangeService,
) {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    @PostMapping("/room")
    @PreAuthorize("isAuthenticated()")
    fun getOrCreateChatRoom(
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam friendEmail: String,
    ): ResponseEntity<ChatRoomDTO> {
        val userEmail = user.username
        val chatRoom = chatService.getOrCreateChatRoom(listOf(userEmail, friendEmail))
        return ResponseEntity.ok(chatRoom)
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    fun sendMessage(
        @RequestParam chatRoomId: Long,
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam message: String,
    ): ResponseEntity<ChatMessageDTO> {
        val senderEmail = user.username
        val chatMessage = chatService.sendMessage(chatRoomId, senderEmail, message)
        return ResponseEntity.ok(chatMessage)
    }

    @GetMapping("/{chatRoomId}/messages")
    @PreAuthorize("isAuthenticated()")
    fun getChatHistory(
        @PathVariable chatRoomId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<List<ChatMessageDTO>> {
        logger.info("Fetching chat history for chatRoomId=$chatRoomId")
        val messages = chatService.getChatHistory(chatRoomId, user.username)
        return ResponseEntity.ok(messages)
    }

    @PostMapping("/key-exchange", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun savePublicKey(
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam publicKey: String,
    ): ResponseEntity<ApiMessageResponseDTO> {
        val userEmail = user.username
        keyExchangeService.saveUserPublicKey(userEmail, publicKey)
        return ResponseEntity.ok(ApiMessageResponseDTO("Public key saved successfully."))
    }

    @GetMapping("/key-exchange/me")
    @PreAuthorize("isAuthenticated()")
    fun getMyPublicKey(@AuthenticationPrincipal user: UserDetails): ResponseEntity<String> {
        return getPublicKeyResponse(user.username)
    }

    @Deprecated(message = "Use /me variant")
    @Operation(deprecated = true, summary = "Deprecated alias for /chat/key-exchange/me")
    @GetMapping("/key-exchange/{email}")
    @PreAuthorize("isAuthenticated()")
    @Hidden
    fun getPublicKey(
        @PathVariable email: String,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<String> {
        if (email != user.username) {
            throw UnauthorizedAccessException(
                "Deprecated endpoint only supports the authenticated principal; use /chat/key-exchange/me"
            )
        }
        return getMyPublicKey(user)
    }

    private fun getPublicKeyResponse(email: String): ResponseEntity<String> {
        val publicKey = keyExchangeService.getUserPublicKey(email) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(publicKey)
    }
}
