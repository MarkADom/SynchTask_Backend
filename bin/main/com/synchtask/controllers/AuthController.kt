package com.synchtask.controllers

import com.synchtask.dtos.user.UserLoginDTO
import com.synchtask.dtos.user.UserRegistrationDTO
import com.synchtask.dtos.user.UserResponseDTO
import com.synchtask.entities.UserRole
import com.synchtask.managers.AuthManager
import com.synchtask.security.JwtKeyManager
import com.synchtask.services.auth.AuthService
import com.synchtask.services.auth.RefreshTokenService
import com.synchtask.services.user.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * **Authentication Controller**
 *
 * Provides authentication endpoints for user registration, login, OAuth2 authentication,
 * refresh tokens, and secure logout. Supports JWT-based authentication.
 */
@RestController
@RequestMapping("/auth")
@Validated
class AuthController(
    private val authManager: AuthManager,
    private val jwtKeyManager: JwtKeyManager,
    private val userService: UserService,
    private val refreshTokenService: RefreshTokenService,
    private val authService: AuthService
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthController::class.java)
    }

    /**
     * Registers a new user.
     * Public endpoint — any user can register and becomes OWNER by default.
     */
    @PostMapping("/register")
    fun registerUser(@RequestBody signUpRequest: UserRegistrationDTO): ResponseEntity<UserResponseDTO> {
        logger.info("Registering new user: ${signUpRequest.email}")

        val newUser = authManager.registerUser(signUpRequest)
        logger.info("User successfully registered: ${newUser.email}")

        return ResponseEntity.status(HttpStatus.CREATED).body(
            UserResponseDTO(
                id = newUser.id ?: throw IllegalArgumentException("User ID cannot be null"),
                name = newUser.name,
                email = newUser.email,
                profilePictureUrl = newUser.profilePictureUrl ?: "N/A"
            )
        )
    }

    /**
     * Authenticates a user and issues JWT and Refresh Token.
     */
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: UserLoginDTO): ResponseEntity<Map<String, Any>> {
        logger.info("Login attempt for email: ${loginRequest.email}")

        return try {
            val tokens = authManager.authenticateUser(loginRequest.email, loginRequest.password)
            val user = userService.getUserByEmail(loginRequest.email)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

            val userDto = UserResponseDTO(
                id = user.id ?: throw IllegalArgumentException("User ID cannot be null"),
                name = user.name,
                email = user.email,
                profilePictureUrl = user.profilePictureUrl ?: "N/A"
            )

            val responseBody = mapOf(
                "accessToken" to tokens["accessToken"]!!,
                "refreshToken" to tokens["refreshToken"]!!,
                "user" to userDto
            )

            ResponseEntity.ok(responseBody)
        } catch (e: SecurityException) {
            logger.warn("Authentication failed for user: ${loginRequest.email}", e)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials", e)
        }
    }

    /**
     * OAuth2 login success handler.
     */
    @GetMapping("/oauth2/success")
    fun getOAuth2User(@AuthenticationPrincipal principal: OAuth2User): ResponseEntity<Map<String, Any>> {
        logger.info("OAuth2 authentication successful for user: ${principal.attributes["email"]}")
        return ResponseEntity.ok(principal.attributes)
    }

    /**
     * OIDC login success handler.
     */
    @GetMapping("/oidc/success")
    fun getOidcUser(@AuthenticationPrincipal oidcUser: OidcUser): ResponseEntity<Map<String, Any>> {
        logger.info("OIDC authentication successful for user: ${oidcUser.email}")
        return ResponseEntity.ok(oidcUser.claims)
    }

    /**
     * Handles secure logout.
     * Revokes all active refresh tokens for the authenticated user.
     */
    @DeleteMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal user: UserDetails
    ) {
        val email = user.username
        val userEntity = userService.getUserByEmail(email)

        if (userEntity != null) {
            refreshTokenService.revokeTokensForUser(userEntity)
            logger.info("User logged out, refresh tokens revoked: $email")
        } else {
            logger.warn("Logout attempted but user not found: $email")
        }

        SecurityContextLogoutHandler().logout(request, response, null)
        response.status = HttpServletResponse.SC_OK
    }

    /**
     * Returns the JSON Web Key Set (JWKS) for public verification.
     */
    @GetMapping("/jwks")
    fun getJwks(): Map<String, Any> = jwtKeyManager.getJwks()

    /**
     * Refresh JWT token using a valid refresh token.
     */
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: Map<String, String>): ResponseEntity<Map<String, String>> {
        val refreshToken = request["refreshToken"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required")

        val newAccessToken = authManager.refreshJwt(refreshToken)
        return ResponseEntity.ok(mapOf("accessToken" to newAccessToken))
    }

    /**
     * Updates the role of a user.
     * Only ADMINs can change roles — assigning ADMIN is explicitly blocked.
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun updateUserRole(
        @AuthenticationPrincipal adminUser: UserDetails,
        @PathVariable userId: Long,
        @RequestParam newRole: UserRole
    ): ResponseEntity<String> {
        if (newRole == UserRole.ADMIN) {
            throw IllegalArgumentException("Assigning 'ADMIN' role is blocked via API.")
        }

        authService.updateUserRole(adminUser.username, userId, newRole)
        return ResponseEntity.ok("User role updated successfully")
    }
}
