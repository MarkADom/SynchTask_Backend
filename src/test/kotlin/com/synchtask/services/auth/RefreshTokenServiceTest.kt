package com.synchtask.services.auth

import com.synchtask.security.domain.entity.RefreshToken
import com.synchtask.user.domain.entity.User
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.security.application.service.RefreshTokenService
import com.synchtask.security.domain.repository.RefreshTokenRepository
import io.mockk.*
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenServiceTest {

    private val refreshTokenRepository: RefreshTokenRepository = mockk(relaxed = true)
    private lateinit var refreshTokenService: RefreshTokenService

    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        refreshTokenService = RefreshTokenService(refreshTokenRepository)

        testUser = User(
            id = 1L,
            name = "Marco",
            email = "marco@example.com",
            passwordHash = "hashedpassword",
            profilePictureUrl = null
        )
    }

    @Test
    fun `should create a new refresh token`() {
        every { refreshTokenRepository.save(any<RefreshToken>()) } answers { firstArg() }

        val token = refreshTokenService.createRefreshToken(testUser)

        assertEquals(testUser, token.user)
        assertTrue(token.expiryDate.isAfter(LocalDateTime.now()))
        assertEquals(false, token.isRevoked)
        verify(exactly = 1) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `should validate a valid refresh token`() {
        val token = RefreshToken(
            user = testUser,
            token = "valid-token",
            expiryDate = LocalDateTime.now().plusDays(1),
            isRevoked = false
        )

        every { refreshTokenRepository.findByToken("valid-token") } returns java.util.Optional.of(token)

        val result = refreshTokenService.validateRefreshToken("valid-token")

        assertEquals(token, result)
    }

    @Test
    fun `should throw if refresh token is revoked`() {
        val token = RefreshToken(
            user = testUser,
            token = "revoked-token",
            expiryDate = LocalDateTime.now().plusDays(1),
            isRevoked = true
        )

        every { refreshTokenRepository.findByToken("revoked-token") } returns java.util.Optional.of(token)

        val exception = assertFailsWith<IllegalArgumentException> {
            refreshTokenService.validateRefreshToken("revoked-token")
        }

        assertEquals("Refresh token is revoked", exception.message)
    }

    @Test
    fun `should throw if refresh token is expired`() {
        val token = RefreshToken(
            user = testUser,
            token = "expired-token",
            expiryDate = LocalDateTime.now().minusDays(1),
            isRevoked = false
        )

        every { refreshTokenRepository.findByToken("expired-token") } returns java.util.Optional.of(token)

        val exception = assertFailsWith<IllegalArgumentException> {
            refreshTokenService.validateRefreshToken("expired-token")
        }

        assertEquals("Refresh token is expired", exception.message)
    }

    @Test
    fun `should revoke a specific refresh token`() {
        val token = RefreshToken(
            user = testUser,
            token = "to-revoke",
            expiryDate = LocalDateTime.now().plusDays(7),
            isRevoked = false
        )

        every { refreshTokenRepository.findByToken("to-revoke") } returns java.util.Optional.of(token)
        every { refreshTokenRepository.save(any()) } returns token

        refreshTokenService.revokeToken("to-revoke")

        assertTrue(token.isRevoked)
        verify { refreshTokenRepository.save(match { it.token == "to-revoke" && it.isRevoked }) }
    }

    @Test
    fun `should revoke all tokens for a user`() {
        val tokens = listOf(
            RefreshToken(user = testUser, token = "token1", expiryDate = LocalDateTime.now().plusDays(1)),
            RefreshToken(user = testUser, token = "token2", expiryDate = LocalDateTime.now().plusDays(1)),
        )

        every { refreshTokenRepository.findAllByUserAndIsRevokedFalse(testUser) } returns tokens
        every { refreshTokenRepository.saveAll(any<List<RefreshToken>>()) } returnsArgument 0

        refreshTokenService.revokeTokensForUser(testUser)

        assertTrue(tokens.all { it.isRevoked })
        verify {
            refreshTokenRepository.saveAll(match<List<RefreshToken>> { list ->
                list.all { it.isRevoked }
            })
        }

    }

    @Test
    fun `should do nothing if user has no active tokens`() {
        every { refreshTokenRepository.findAllByUserAndIsRevokedFalse(testUser) } returns emptyList()

        refreshTokenService.revokeTokensForUser(testUser)

        verify(exactly = 0) { refreshTokenRepository.saveAll(any<List<RefreshToken>>()) }
    }

    @Test
    fun `should throw if token not found during revoke`() {
        every { refreshTokenRepository.findByToken("missing-token") } returns java.util.Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            refreshTokenService.revokeToken("missing-token")
        }
    }

    @Test
    fun `should throw if token not found during validate`() {
        every { refreshTokenRepository.findByToken("missing-token") } returns java.util.Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            refreshTokenService.validateRefreshToken("missing-token")
        }
    }
}
