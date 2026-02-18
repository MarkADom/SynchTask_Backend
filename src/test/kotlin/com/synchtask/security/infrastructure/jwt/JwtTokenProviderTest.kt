package com.synchtask.security.infrastructure.jwt

import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import java.util.*

class JwtTokenProviderTest {
    private lateinit var jwtKeyManager: JwtKeyManager
    private lateinit var userRepository: UserRepository
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var tokenProvider: JwtTokenProvider

    private val expiration = 60_000L
    private val issuer = "synchtask-api"
    private val audience = "synchtask-client"

    @BeforeEach
    fun setup() {
        jwtKeyManager = JwtKeyManager(TestKeyPairs.generateRsa())
        userRepository = mockk()
        userDetailsService = mockk()

        tokenProvider =
            JwtTokenProvider(
                jwtKeyManager = jwtKeyManager,
                userDetailsService = userDetailsService,
                userRepository = userRepository,
                expiration = expiration,
                issuer = issuer,
                audience = audience
            )
    }

    @Test
    fun `should generate valid JWT token`() {
        val user =
            User(
                id = 1L,
                name = "Admin",
                email = "admin@synchtask.com",
                passwordHash = "hash",
                role = UserRole.ADMIN
            )

        val userDetails: UserDetails =
            org.springframework.security.core.userdetails.User(
                user.email,
                user.passwordHash,
                listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
            )

        every { userRepository.findByEmail(user.email) } returns Optional.of(user)

        val token = tokenProvider.generateToken(userDetails)

        assertNotNull(token)
        assertTrue(token.split(".").size == 3) // JWT format
    }

    @Test
    fun `should validate token and extract user`() {
        val user =
            User(
                id = 1L,
                name = "Admin",
                email = "admin@synchtask.com",
                passwordHash = "hash",
                role = UserRole.ADMIN
            )

        val userDetails =
            org.springframework.security.core.userdetails.User(
                user.email,
                user.passwordHash,
                listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
            )

        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { userDetailsService.loadUserByUsername(user.email) } returns userDetails

        val token = tokenProvider.generateToken(userDetails)

        val extracted = tokenProvider.validateAndExtractUser(token)

        assertNotNull(extracted)
        assertEquals(user.email, extracted!!.username)
        assertEquals(1, extracted.authorities.size)
    }

    @Test
    fun `should return null for invalid token`() {
        val invalidToken = "invalid.jwt.token"

        val result = tokenProvider.validateAndExtractUser(invalidToken)

        assertNull(result)
    }

    @Test
    fun `should return null if user not found during validation`() {
        val user =
            User(
                id = 1L,
                name = "User",
                email = "ghost@synchtask.com",
                passwordHash = "hash",
                role = UserRole.USER
            )

        val userDetails =
            org.springframework.security.core.userdetails.User(
                user.email,
                user.passwordHash,
                listOf(SimpleGrantedAuthority("ROLE_USER"))
            )

        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { userDetailsService.loadUserByUsername(user.email) } throws RuntimeException("User missing")

        val token = tokenProvider.generateToken(userDetails)

        val extracted = tokenProvider.validateAndExtractUser(token)

        assertNull(extracted)
    }

    @Test
    fun `should return null if user has no roles`() {
        val user =
            User(
                id = 1L,
                name = "User",
                email = "norole@synchtask.com",
                passwordHash = "hash",
                role = UserRole.USER
            )

        val userDetails =
            org.springframework.security.core.userdetails.User(
                user.email,
                user.passwordHash,
                emptyList()
            )

        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { userDetailsService.loadUserByUsername(user.email) } returns userDetails

        val token = tokenProvider.generateToken(userDetails)

        val extracted = tokenProvider.validateAndExtractUser(token)

        assertNull(extracted)
    }
}
