package com.synchtask.controllers

import com.synchtask.dtos.chat.ChatMessageDTO
import com.synchtask.dtos.chat.ChatRoomDTO
import com.synchtask.services.chat.ChatService
import com.synchtask.services.chat.KeyExchangeService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * **Chat Controller**
 *
 * Handles chat operations such as message sending, room creation,
 * encrypted key exchange, and history retrieval.
 */
@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
    private val keyExchangeService: KeyExchangeService
) {

    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    /**
     * Creates or retrieves a private chat room between the authenticated user and a friend.
     */
    @PostMapping("/room")
    @PreAuthorize("isAuthenticated()")
    fun getOrCreateChatRoom(
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam friendEmail: String
    ): ResponseEntity<ChatRoomDTO> {
        val userEmail = user.username
        val chatRoom = chatService.getOrCreateChatRoom(listOf(userEmail, friendEmail))
        return ResponseEntity.ok(chatRoom)
    }

    /**
     * Sends a text message within a chat room.
     */
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    fun sendMessage(
        @RequestParam chatRoomId: Long,
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam message: String
    ): ResponseEntity<ChatMessageDTO> {
        val senderEmail = user.username
        val chatMessage = chatService.sendMessage(chatRoomId, senderEmail, message)
        return ResponseEntity.ok(chatMessage)
    }

    /**
     * Retrieves the message history from a chat room.
     */
    @GetMapping("/{chatRoomId}/messages")
    @PreAuthorize("isAuthenticated()")
    fun getChatHistory(
        @PathVariable chatRoomId: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<ChatMessageDTO>> {
        logger.info("Fetching chat history for chatRoomId=$chatRoomId")
        val messages = chatService.getChatHistory(chatRoomId)
        return ResponseEntity.ok(messages)
    }

    /**
     * Saves the user's public key for encrypted communication.
     */
    @PostMapping("/key-exchange")
    @PreAuthorize("isAuthenticated()")
    fun savePublicKey(
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam publicKey: String
    ): ResponseEntity<String> {
        val userEmail = user.username
        keyExchangeService.saveUserPublicKey(userEmail, publicKey)
        return ResponseEntity.ok("Public key saved successfully.")
    }

    /**
     * Retrieves the stored public key of another user for encrypted communication.
     */
    @GetMapping("/key-exchange/{email}")
    @PreAuthorize("isAuthenticated()")
    fun getPublicKey(@PathVariable email: String): ResponseEntity<String> {
        val publicKey = keyExchangeService.getUserPublicKey(email)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(publicKey)
    }
}
