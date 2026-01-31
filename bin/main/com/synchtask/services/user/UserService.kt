package com.synchtask.services.user

import com.synchtask.dtos.user.UserResponseDTO
import com.synchtask.dtos.user.UserStatusDTO
import com.synchtask.entities.User
import com.synchtask.entities.UserRole
import com.synchtask.exception.ResourceNotFoundException
import com.synchtask.exception.UserAlreadyExistsException
import com.synchtask.mappers.UserMapper
import com.synchtask.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID


/**
 * **User Service**
 *
 * Manages all user-related operations, including registration, updates, status, and access logic.
 */
@Service
@Suppress("TooManyFunctions")
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Registers a new user, applying password hashing if needed.
     *
     * @throws UserAlreadyExistsException if email already exists.
     */
    fun createUser(user: User): User {
        if (userRepository.findByEmail(user.email).isPresent) {
            throw UserAlreadyExistsException("User with email ${user.email} already exists")
        }

        val rawPassword = user.passwordHash
        val encodedPassword = if (rawPassword.startsWith("\$2a\$")) {
            logger.warn("Password already encoded: skipping hashing")
            rawPassword
        } else {
            passwordEncoder.encode(rawPassword)
        }

        val secureUser = user.copy(passwordHash = encodedPassword)
        logger.info("User registered: ${secureUser.email}")
        return userRepository.save(secureUser)
    }

    /**
     * Retrieves a user by ID.
     */
    fun getUserById(id: Long): User? = userRepository.findById(id).orElse(null)

    /**
     * Retrieves a user by email.
     */
    fun getUserByEmail(email: String): User? = userRepository.findByEmail(email).orElse(null)

    /**
     * Updates user details (only editable fields).
     */
    @Transactional
    fun updateUser(id: Long, userDetails: User): User? {
        val user = getUserById(id) ?: return null

        user.name = userDetails.name
        user.email = userDetails.email

        val newPassword = userDetails.passwordHash
        if (!newPassword.startsWith("\$2a\$")) {
            user.passwordHash = passwordEncoder.encode(newPassword)
        }

        user.profilePictureUrl = userDetails.profilePictureUrl ?: user.profilePictureUrl
        logger.info("User updated: ${user.email}")
        return userRepository.save(user)
    }

    /**
     * Deletes a user by ID.
     */
    @Transactional
    fun deleteUser(id: Long) {
        logger.info("Deleting user ID: $id")
        userRepository.deleteById(id)
    }

    /**
     * Checks if the user has ADMIN role.
     */
    fun isAdmin(email: String): Boolean =
        userRepository.findByEmail(email).orElse(null)?.role == UserRole.ADMIN

    /**
     * Validates whether the user has access to update/delete/view another user.
     */
    fun isAuthorized(authenticatedEmail: String, targetEmail: String): Boolean {
        return authenticatedEmail == targetEmail || isAdmin(authenticatedEmail)
    }

    /**
     * Updates online status and last activity of a user.
     */
    @Transactional
    fun setUserOnlineStatus(email: String, isOnline: Boolean) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.isOnline = isOnline
        if (!isOnline) {
            user.lastActivity = LocalDateTime.now()
        }

        logger.info("User ${user.email} marked as ${if (isOnline) "online" else "offline"}")
        userRepository.save(user)
    }

    /**
     * Refreshes the user's last seen activity.
     */
    @Transactional
    fun updateLastActivity(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.lastActivity = LocalDateTime.now()
        userRepository.save(user)
        logger.info("User ${user.email} last activity updated")
    }

    /**
     * Checks if a user is currently online.
     */
    fun isUserOnline(email: String): Boolean =
        userRepository.findByEmail(email).orElse(null)?.isOnline ?: false

    /**
     * Returns all users currently marked online.
     */
    @Transactional(readOnly = true)
    fun getOnlineUsers(): List<UserStatusDTO> {
        val online = userRepository.findAllByIsOnlineTrue().map {
            UserStatusDTO(
                email = it.email,
                name = it.name,
                lastActivity = it.lastActivity ?: LocalDateTime.now()
            )
        }
        logger.info("Fetched ${online.size} online users")
        return online
    }

    /**
     * Returns all users as full response DTOs.
     */
    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserResponseDTO> {
        logger.info("Fetching all users")
        return userRepository.findAll().map(UserMapper::toResponseDTO)
    }

    /**
     * Returns users eligible for task assignment.
     */
    @Transactional(readOnly = true)
    fun getAssignableUsers(): List<User> {
        return userRepository.findAll()
            .filter { it.role in listOf(UserRole.ADMIN, UserRole.OWNER, UserRole.COLLABORATOR, UserRole.USER) }
    }

    fun getAllUsersRaw(): List<User> {
        logger.info("Fetching all users (raw)")
        return userRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findPublicUsers(name: String?, onlineOnly: Boolean?, pageable: Pageable): Page<User> {
        return userRepository.findUsersByFilters(name, onlineOnly, pageable)
    }

    /**
     * Updates profile picture of a user and persists the file locally.
     *
     * @param email Email of the authenticated user.
     * @param file Uploaded multipart file containing the profile picture.
     * @return Updated User entity with profile picture URL.
     */
    @Transactional
    fun updateProfilePicture(email: String, file: MultipartFile): User {
        val user = userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val uploadDir = Paths.get("uploads/profile-pictures/")
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir)
        }

        val extension = file.originalFilename?.substringAfterLast('.', "jpg")
        val filename = "${user.id}_${UUID.randomUUID()}.$extension"
        val filePath = uploadDir.resolve(filename)

        Files.copy(file.inputStream, filePath)

        val uploadedUrl = "/static/profile-pictures/$filename"
        user.profilePictureUrl = uploadedUrl

        logger.info("Profile picture updated for user: ${user.email}")
        return userRepository.save(user)
    }
}

