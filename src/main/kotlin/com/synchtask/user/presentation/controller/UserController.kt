package com.synchtask.user.presentation.controller

import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.user.presentation.mapper.UserMapper
import com.synchtask.friend.application.service.FriendService
import com.synchtask.user.application.dto.UpdateUserDTO
import com.synchtask.user.application.dto.UserOptionDTO
import com.synchtask.user.application.dto.UserPublicDTO
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.application.dto.UserStatusDTO
import com.synchtask.user.application.service.UserService
import com.synchtask.user.presentation.mapper.UserCommandMapper
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * User-related HTTP endpoints.
 *
 * Authorization rules are enforced via Spring Security and service-level checks.
 */
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "BearerAuth")
class UserController(
    private val userService: UserService,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val friendService: FriendService,
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun createUser(@RequestBody userRegistrationDTO: UserRegistrationDTO): ResponseEntity<UserResponseDTO> {
        val newUser = userService.createUser(
            UserCommandMapper.toNewUser(
                dto = userRegistrationDTO,
                passwordEncoder = passwordEncoder
            )
        )
        logger.info("User created successfully: ${newUser.email}")
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponseDTO(newUser))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserResponseDTO> {
        val user = userService.getUserById(id)
            ?: throw ResourceNotFoundException("User not found with ID: $id")
        return ResponseEntity.ok(UserMapper.toResponseDTO(user))
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    fun getAllUsers(): ResponseEntity<List<UserResponseDTO>> {
        val users = userService.getAllUsers()
        return ResponseEntity.ok(users)
    }

    @GetMapping("/assignable")
    @PreAuthorize("isAuthenticated()")
    fun getAssignableUsers(): ResponseEntity<List<UserOptionDTO>> {
        val users = userService.getAssignableUsers()
        return ResponseEntity.ok(users.map(UserMapper::toOptionDTO))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody updateUserDTO: UpdateUserDTO,
        @AuthenticationPrincipal user: UserDetails,
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
                val updatedUser =
                    userService.updateUser(id, UserCommandMapper.toUpdatedUser(updateUserDTO, existingUser))
                        ?: return handleUserNotFound(id, userEmail)

                logger.info("User updated successfully: id=$id by admin=${userEmail}")
                ResponseEntity.ok(UserMapper.toResponseDTO(updatedUser))
            }
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    fun deleteUser(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails,
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

    @GetMapping("/online")
    @PreAuthorize("isAuthenticated()")
    fun getOnlineUsers(): ResponseEntity<List<UserStatusDTO>> {
        val onlineUsers = userService.getOnlineUsers()
        return ResponseEntity.ok(onlineUsers)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal user: UserDetails): ResponseEntity<UserResponseDTO> {
        val currentUser = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("Authenticated user not found")
        return ResponseEntity.ok(UserMapper.toResponseDTO(currentUser))
    }

    @GetMapping("/public")
    @PreAuthorize("isAuthenticated()")
    fun getPublicUsers(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) onlineOnly: Boolean?,
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable,
    ): ResponseEntity<Page<UserPublicDTO>> {
        val users = userService.findPublicUsers(name, onlineOnly, pageable)
        return ResponseEntity.ok(users.map(UserMapper::toPublicDTO))
    }

    private fun handleUserNotFound(userId: Long, requesterEmail: String): ResponseEntity<UserResponseDTO> {
        logger.warn("User not found with ID: $userId by $requesterEmail")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun updateCurrentUser(
        @RequestBody updateUserDTO: UpdateUserDTO,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<UserResponseDTO> {
        val currentUser = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("Authenticated user not found")

        val updatedUser =
            userService.updateUser(currentUser.id!!, UserCommandMapper.toUpdatedUser(updateUserDTO, currentUser))
                ?: throw ResourceNotFoundException("Failed to update user")

        logger.info("User self-updated successfully: email=${updatedUser.email}")
        return ResponseEntity.ok(UserMapper.toResponseDTO(updatedUser))
    }

    @PutMapping("/me/profile-picture")
    @PreAuthorize("isAuthenticated()")
    fun updateProfilePicture(
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<Map<String, String>> {
        val updatedUser = userService.updateProfilePicture(user.username, file)
        return ResponseEntity.ok(mapOf("profilePictureUrl" to updatedUser.profilePictureUrl.orEmpty()))
    }

    @GetMapping("/visible")
    @PreAuthorize("isAuthenticated()")
    fun getVisibleUsers(@AuthenticationPrincipal user: UserDetails): ResponseEntity<List<UserResponseDTO>> {
        val currentUser = userService.getUserByEmail(user.username)
            ?: throw ResourceNotFoundException("Authenticated user not found")

        val visibleUsers = userService.getVisibleUsers(currentUser, emptyList())

        return ResponseEntity.ok(visibleUsers)
    }
}
