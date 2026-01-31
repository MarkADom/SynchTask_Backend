package com.synchtask.controllers

import com.synchtask.dtos.user.*
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UnauthorizedAccessException
import com.synchtask.mappers.UserMapper
import com.synchtask.services.user.UserService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.domain.Page
import org.springframework.web.multipart.MultipartFile


/**
 * **User Management Controller**
 *
 * Handles user-related operations such as registration, retrieval, updates, and deletion.
 */
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "BearerAuth")
class UserController(
    private val userService: UserService,
    private val passwordEncoder: BCryptPasswordEncoder
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    /**
     * Creates a new user.
     * Only ADMIN users are allowed to create new users through this endpoint.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun createUser(@RequestBody userRegistrationDTO: UserRegistrationDTO): ResponseEntity<UserResponseDTO> {
        val newUser = userService.createUser(userRegistrationDTO.toUser(passwordEncoder))
        logger.info("User created successfully: ${newUser.email}")
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponseDTO(newUser))
    }

    /**
     * Retrieves a user by ID.
     * Only ADMIN or the user themselves can access this endpoint.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserResponseDTO> {
        val user = userService.getUserById(id)
            ?: throw ResourceNotFoundException("User not found with ID: $id")
        return ResponseEntity.ok(UserMapper.toResponseDTO(user))
    }

    /**
     * Retrieves all users.
     * Only ADMIN or OWNER roles are allowed.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    fun getAllUsers(): ResponseEntity<List<UserResponseDTO>> {
        val users = userService.getAllUsers()
        return ResponseEntity.ok(users)
    }

    /**
     * Retrieves assignable users (e.g. for tasks).
     * Any authenticated user can access.
     */
    @GetMapping("/assignable")
    @PreAuthorize("isAuthenticated()")
    fun getAssignableUsers(): ResponseEntity<List<UserOptionDTO>> {
        val users = userService.getAssignableUsers()
        return ResponseEntity.ok(users.map(UserMapper::toOptionDTO))
    }

    /**
     * Updates a user securely.
     * Only ADMIN or the user themselves may update their info.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody updateUserDTO: UpdateUserDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<UserResponseDTO> {
        val userEmail = user.username
        val existingUser = userService.getUserById(id)

        return when {
            existingUser == null -> handleUserNotFound(id, userEmail)
            !userService.isAuthorized(userEmail, existingUser.email) -> {
                logger.warn("Unauthorized update attempt by $userEmail on user ${existingUser.email}")
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
            else -> {
                val updatedUser = userService.updateUser(id, updateUserDTO.toUser(existingUser))
                    ?: return handleUserNotFound(id, userEmail)

                logger.info("User updated successfully: id=$id by admin=${userEmail}")
                ResponseEntity.ok(UserMapper.toResponseDTO(updatedUser))
            }
        }
    }

    /**
     * Deletes a user securely.
     * Only ADMIN or the user themselves may delete their account.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    fun deleteUser(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {
        val userEmail = user.username
        val userToDelete = userService.getUserById(id)
            ?: throw ResourceNotFoundException("User not found with ID: $id")

        if (!userService.isAuthorized(userEmail, userToDelete.email)) {
            logger.warn("Unauthorized delete attempt by $userEmail on user ${userToDelete.email}")
            throw UnauthorizedAccessException("Unauthorized to delete this user")
        }

        userService.deleteUser(id)
        logger.info("User deleted successfully: ${userToDelete.email}")
        return ResponseEntity.noContent().build()
    }

    /**
     * Retrieves online users.
     */
    @GetMapping("/online")
    @PreAuthorize("isAuthenticated()")
    fun getOnlineUsers(): ResponseEntity<List<UserStatusDTO>> {
        val onlineUsers = userService.getOnlineUsers()
        return ResponseEntity.ok(onlineUsers)
    }

    /**
     * Retrieves the current authenticated user.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal user: UserDetails): ResponseEntity<UserResponseDTO> {
        val currentUser = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("Authenticated user not found")
        return ResponseEntity.ok(UserMapper.toResponseDTO(currentUser))
    }

    /**
     * Retrieves public users by name and status.
     */
    @GetMapping("/public")
    @PreAuthorize("isAuthenticated()")
    fun getPublicUsers(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) onlineOnly: Boolean?,
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable
    ): ResponseEntity<Page<UserPublicDTO>> {
        val users = userService.findPublicUsers(name, onlineOnly, pageable)
        return ResponseEntity.ok(users.map(UserMapper::toPublicDTO))
    }

    private fun handleUserNotFound(userId: Long, requesterEmail: String): ResponseEntity<UserResponseDTO> {
        logger.warn("User not found with ID: $userId by $requesterEmail")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    /**
     * Updates current authenticated user.
     *
     * PATCH or PUT to update only your own profile data (name, avatar).
     * Accessible by any authenticated user.
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun updateCurrentUser(
        @RequestBody updateUserDTO: UpdateUserDTO,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<UserResponseDTO> {
        val currentUser = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("Authenticated user not found")

        val updatedUser = userService.updateUser(currentUser.id!!, updateUserDTO.toUser(currentUser))
            ?: throw ResourceNotFoundException("Failed to update user")

        logger.info("User self-updated successfully: email=${updatedUser.email}")
        return ResponseEntity.ok(UserMapper.toResponseDTO(updatedUser))
    }

    /**
    * Uploads and updates authenticated user's profile picture.
    * @param file Multipart image file.
    * @param user Authenticated user principal.
    * @return URL of the uploaded profile picture.
    */
    @PutMapping("/me/profile-picture")
    @PreAuthorize("isAuthenticated()")
    fun updateProfilePicture(
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Map<String, String>> {
        val updatedUser = userService.updateProfilePicture(user.username, file)
        return ResponseEntity.ok(mapOf("profilePictureUrl" to updatedUser.profilePictureUrl.orEmpty()))
    }
}
