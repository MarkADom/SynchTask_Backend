package com.synchtask.security.presentation.controller

import com.synchtask.security.application.dto.AuthLoginResponseDTO
import com.synchtask.security.application.dto.JwksResponseDTO
import com.synchtask.security.application.manager.AuthManager
import com.synchtask.security.application.service.AuthService
import com.synchtask.security.application.dto.OidcUserInfoDTO
import com.synchtask.security.application.dto.RefreshTokenRequestDTO
import com.synchtask.security.application.dto.TokenPairDTO
import com.synchtask.security.infrastructure.jwt.JwtKeyManager
import com.synchtask.security.domain.exception.InvalidCredentialsException
import com.synchtask.shared.dto.ApiMessageResponseDTO
import com.synchtask.user.application.dto.UserLoginDTO
import com.synchtask.user.application.dto.UserRegistrationDTO
import com.synchtask.user.application.dto.UserResponseDTO
import com.synchtask.user.application.service.AuthenticatedUserService
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.UserRole
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
import com.synchtask.shared.exception.InvalidInputException
import com.synchtask.shared.exception.ResourceNotFoundException


/**
 * Authentication endpoints for registration, login and token management.
 *
 * Handles both JWT-based and OAuth2/OIDC authentication flows.
 */
@RestController
@RequestMapping("/auth")
@Validated
class AuthController(
    private val authManager: AuthManager,
    private val jwtKeyManager: JwtKeyManager,
    private val userService: UserService,
    private val authenticatedUserService: AuthenticatedUserService,
    private val authService: AuthService,
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthController::class.java)
    }

    @PostMapping("/register")
    fun registerUser(@RequestBody signUpRequest: UserRegistrationDTO): ResponseEntity<UserResponseDTO> {
        logger.info("Registering new user: ${signUpRequest.email}")

        val newUser = authManager.registerUser(signUpRequest)
        logger.info("User successfully registered: ${newUser.email}")

        return ResponseEntity.status(HttpStatus.CREATED).body(
            UserResponseDTO(
                id = newUser.id ?: throw ResourceNotFoundException("Registered user id is missing"),
                name = newUser.name,
                email = newUser.email,
                profilePictureUrl = newUser.profilePictureUrl ?: "N/A"
            )
        )
    }

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: UserLoginDTO): ResponseEntity<AuthLoginResponseDTO> {
        logger.info("Login attempt for email: ${loginRequest.email}")

        return try {
            val tokens = authManager.authenticateUser(loginRequest.email, loginRequest.password)
            val user =
                userService.getUserByEmail(loginRequest.email)
                    ?: throw ResourceNotFoundException("User not found")

            val userDto =
                UserResponseDTO(
                    id = user.id ?: throw ResourceNotFoundException("User ID cannot be null"),
                    name = user.name,
                    email = user.email,
                    profilePictureUrl = user.profilePictureUrl ?: "N/A"
                )
            ResponseEntity.ok(
                AuthLoginResponseDTO(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken ?: "",
                    user = userDto
                )
            )

        } catch (e: SecurityException) {
            logger.warn("Authentication failed", e)
            throw InvalidCredentialsException("Invalid credentials")
        }
    }

    @GetMapping("/oauth2/success")
    fun getOAuth2User(@AuthenticationPrincipal principal: OAuth2User): ResponseEntity<OidcUserInfoDTO> {
        logger.info("OAuth2 authentication successful")
        return ResponseEntity.ok(
            OidcUserInfoDTO(
                email = principal.attributes["email"]?.toString().orEmpty(),
                name = principal.attributes["name"]?.toString().orEmpty(),
                roles = principal.authorities.map { it.authority }
            )
        )
    }

    @GetMapping("/oidc/success")
    fun getOidcUser(@AuthenticationPrincipal oidcUser: OidcUser): ResponseEntity<OidcUserInfoDTO> {
        logger.info("OIDC authentication successful")
        return ResponseEntity.ok(
            OidcUserInfoDTO(
                email = oidcUser.email ?: "",
                name = oidcUser.fullName ?: oidcUser.subject,
                roles = oidcUser.authorities.map { it.authority }
            )
        )
    }

    @DeleteMapping("/logout", produces = ["application/json"])
    @PreAuthorize("isAuthenticated()")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<ApiMessageResponseDTO> {
        val userEntity = authenticatedUserService.requireUser(user)

        authManager.logoutUser(userEntity.email)

        SecurityContextLogoutHandler().logout(request, response, null)
        response.status = HttpServletResponse.SC_OK

        return ResponseEntity.ok(ApiMessageResponseDTO("Logout successful"))
    }

    @Deprecated("Use /jwks")
    @Operation(deprecated = true, summary = "Deprecated alias for canonical /jwks endpoint")
    @GetMapping("/jwks")
    fun getJwks(): JwksResponseDTO = jwtKeyManager.getJwks()

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequestDTO): ResponseEntity<TokenPairDTO> {
        val newAccessToken = authManager.refreshJwt(request.refreshToken)
        return ResponseEntity.ok(TokenPairDTO(accessToken = newAccessToken))
    }

    /**
     * Allows admins to update user roles.
     *
     * Assigning ADMIN is intentionally blocked via API.
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun updateUserRole(
        @AuthenticationPrincipal adminUser: UserDetails,
        @PathVariable userId: Long,
        @RequestParam newRole: UserRole,
    ): ResponseEntity<ApiMessageResponseDTO> {
        if (newRole == UserRole.ADMIN) {
            throw InvalidInputException("Assigning 'ADMIN' role is blocked via API.")
        }

        authService.updateUserRole(adminUser.username, userId, newRole)
        return ResponseEntity.ok(ApiMessageResponseDTO("User role updated successfully"))
    }
}
