package com.synchtask.services.user

import com.synchtask.entities.UserRole
import com.synchtask.repositories.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.*
import com.synchtask.entities.User as AppUser

class UserDetailsServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userDetailsService: UserDetailsService

    private val email = "user@example.com"

    private val activeUser = AppUser(
        id = 1L,
        name = "Test User",
        email = email,
        passwordHash = "hashedpassword123",
        isActive = true,
        role = UserRole.USER
    )

    private val inactiveUser = AppUser(
        id = 2L,
        name = "Disabled User",
        email = "disabled@example.com",
        passwordHash = "disabledhash",
        isActive = false,
        role = UserRole.USER
    )

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        userDetailsService = CustomUserDetailsService(userRepository)
    }

    @Test
    fun `should load user details successfully when user is active`() {
        every { userRepository.findByEmail(email) } returns Optional.of(activeUser)

        val userDetails = userDetailsService.loadUserByUsername(email)

        assertEquals(email, userDetails.username)
        assertEquals(activeUser.passwordHash, userDetails.password)
        assertTrue(userDetails.authorities.any { it.authority == UserRole.USER.name })
    }

    @Test
    fun `should throw UsernameNotFoundException when user does not exist`() {
        every { userRepository.findByEmail(email) } returns Optional.empty()

        val exception = assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername(email)
        }

        assertEquals("User not found: $email", exception.message)
    }

    @Test
    fun `should throw DisabledException when user is not active`() {
        every { userRepository.findByEmail(inactiveUser.email) } returns Optional.of(inactiveUser)

        val exception = assertThrows(DisabledException::class.java) {
            userDetailsService.loadUserByUsername(inactiveUser.email)
        }

        assertEquals("User is disabled: ${inactiveUser.email}", exception.message)
    }
}
