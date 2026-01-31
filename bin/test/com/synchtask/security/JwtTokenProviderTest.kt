package com.synchtask.security


import com.synchtask.entities.User
import com.synchtask.repositories.UserRepository
import io.jsonwebtoken.Jwts
import io.mockk.*
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import java.util.*
import javax.crypto.SecretKey

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtTokenProviderTest {

    private val jwtKeyManager = mockk<JwtKeyManager>()
    private val userDetailsService = mockk<UserDetailsService>()
    private val userRepository = mockk<UserRepository>()

    // Key com 512 bits (64 bytes)
    private val secureSecretKey: SecretKey = Jwts.SIG.HS512.key().build()
    private val encodedSecret = Base64.getEncoder().encodeToString(secureSecretKey.encoded)

    private val expiration = 3600000L
    private val issuer = "synchtask"
    private val audience = "synchtask-users"

    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setup() {
        clearAllMocks()

        every { jwtKeyManager.isRSA() } returns false
        jwtTokenProvider = spyk(
            JwtTokenProvider(
                jwtKeyManager,
                userDetailsService,
                userRepository,
                encodedSecret,
                expiration,
                issuer,
                audience
            )
        )
    }

    @Test
    fun `should generate and validate token`() {
        val email = "test@synchtask.com"
        val roles = listOf(SimpleGrantedAuthority("ROLE_USER"))

        val userDetails = mockk<UserDetails> {
            every { username } returns email
            every { authorities } returns roles
        }

        val user = mockk<User> {
            every { this@mockk.email } returns email
        }

        every { userRepository.findByEmail(email) } returns Optional.of(user)

        val token = jwtTokenProvider.generateToken(userDetails)

        val parsed = Jwts.parser()
            .verifyWith(secureSecretKey) // se estiver usando HS512 no teste
            .build()
            .parseSignedClaims(token)

        assertEquals(email, parsed.payload.subject)
        assertEquals(issuer, parsed.payload.issuer)

        // ✅ Corrigido: audience como Collection
        val audienceClaim = parsed.payload["aud"]
        assertTrue((audienceClaim as? Collection<*>)?.contains(audience) == true)

        val rolesClaim = parsed.payload["roles"]
        assertTrue((rolesClaim as? Collection<*>)?.contains("ROLE_USER") == true)
    }


    @Test
    fun `should extract token from Authorization header`() {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("Authorization") } returns "Bearer abc.def.ghi"
        every { request.getParameter("token") } returns null

        val token = jwtTokenProvider.extractTokenFromRequest(request)
        assertEquals("abc.def.ghi", token)
    }

    @Test
    fun `should extract token from query parameter`() {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("Authorization") } returns null
        every { request.getParameter("token") } returns "jwt.from.query"

        val token = jwtTokenProvider.extractTokenFromRequest(request)
        assertEquals("jwt.from.query", token)
    }

    @Test
    fun `should return null when token is invalid`() {
        every { jwtKeyManager.isRSA() } returns false
        val invalidToken = "invalid.token.value"

        val result = jwtTokenProvider.validateAndExtractUser(invalidToken)
        assertNull(result)
    }

    @Test
    fun `should return null when user has no roles`() {
        val email = "no.roles@synchtask.com"
        val token = jwtTokenProvider.generateToken(
            mockk<UserDetails> {
                every { username } returns email
                every { authorities } returns emptyList()
            }.also {
                every { userRepository.findByEmail(email) } returns Optional.of(mockk {
                    every { this@mockk.email } returns email
                })
            }
        )

        every { userDetailsService.loadUserByUsername(email) } returns mockk {
            every { authorities } returns emptyList()
        }

        val result = jwtTokenProvider.validateAndExtractUser(token)
        assertNull(result)
    }
}
