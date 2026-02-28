package com.synchtask.user.application.service

import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.User as SpringUser

class AuthenticatedUserServiceTest {
    private val userService = mockk<UserService>()
    private val authenticatedUserService = AuthenticatedUserService(userService)

    @Test
    fun `requireUser returns user when found`() {
        val principal = SpringUser("found@test.com", "pwd", emptyList())
        val user = User(id = 1L, name = "Found", email = "found@test.com", passwordHash = "hash")
        every { userService.getUserByEmail("found@test.com") } returns user

        val result = authenticatedUserService.requireUser(principal)

        assertEquals(user, result)
    }

    @Test
    fun `requireUser throws when user not found`() {
        val principal = SpringUser("missing@test.com", "pwd", emptyList())
        every { userService.getUserByEmail("missing@test.com") } returns null

        assertThrows(ResourceNotFoundException::class.java) {
            authenticatedUserService.requireUser(principal)
        }
    }
}
