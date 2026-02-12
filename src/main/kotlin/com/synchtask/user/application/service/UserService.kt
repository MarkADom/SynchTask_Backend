package com.synchtask.user.application.service

import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.application.dto.UserStatusDTO
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.exception.UserAlreadyExistsException
import com.synchtask.user.presentation.mapper.UserMapper
import com.synchtask.user.domain.repository.UserRepository
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

@Service
@Suppress("TooManyFunctions")
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

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

        val secureUser = User(
            id = user.id,
            name = user.name,
            email = user.email,
            passwordHash = encodedPassword,
            profilePictureUrl = user.profilePictureUrl,
            role = user.role,
            createdAt = user.createdAt,
            lastLogin = user.lastLogin,
            lastActivity = user.lastActivity,
            isActive = user.isActive,
            isOnline = user.isOnline,
            onboardingNotified = user.onboardingNotified
        )

        logger.info("User registered: ${secureUser.email}")
        return userRepository.save(secureUser)
    }

    fun getUserById(id: Long): User? = userRepository.findById(id).orElse(null)

    fun getUserByEmail(email: String): User? = userRepository.findByEmail(email).orElse(null)

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

    @Transactional
    fun deleteUser(id: Long) {
        logger.info("Deleting user ID: $id")
        userRepository.deleteById(id)
    }

    fun isAdmin(email: String): Boolean =
        userRepository.findByEmail(email).orElse(null)?.role == UserRole.ADMIN

    fun isAuthorized(authenticatedEmail: String, targetEmail: String): Boolean {
        return authenticatedEmail == targetEmail || isAdmin(authenticatedEmail)
    }

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

    @Transactional
    fun updateLastActivity(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.lastActivity = LocalDateTime.now()
        userRepository.save(user)
        logger.info("User ${user.email} last activity updated")
    }

    fun isUserOnline(email: String): Boolean =
        userRepository.findByEmail(email).orElse(null)?.isOnline ?: false

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

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserResponseDTO> {
        logger.info("Fetching all users")
        return userRepository.findAll().map(UserMapper::toResponseDTO)
    }

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

    @Transactional(readOnly = true)
    fun getVisibleUsers(currentUser: User, friends: List<User>): List<UserResponseDTO> {
        val visibleUsers = if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.OWNER) {
            userRepository.findAll()
        } else {
            val assignable = getAssignableUsers()
            (friends + assignable).distinctBy { it.id }
        }
        return visibleUsers.map(UserMapper::toResponseDTO)
    }


}

