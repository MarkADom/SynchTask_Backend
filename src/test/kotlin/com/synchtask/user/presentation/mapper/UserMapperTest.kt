package com.synchtask.user.presentation.mapper

import com.synchtask.user.domain.entity.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UserMapperTest {
    @Test
    fun `should map User to UserResponseDTO correctly`() {
        val user =
            User(
                id = 1L,
                name = "Jane Doe",
                email = "jane@example.com",
                passwordHash = "secure123",
                profilePictureUrl = "http://example.com/profile.jpg"
            )

        val dto = UserMapper.toResponseDTO(user)

        Assertions.assertEquals(1L, dto.id)
        Assertions.assertEquals("Jane Doe", user.name)
        Assertions.assertEquals("jane@example.com", user.email)
        Assertions.assertNotNull(dto.profilePictureUrl)
        Assertions.assertTrue(dto.profilePictureUrl!!.isNotBlank())
    }

    @Test
    fun `should map User with null profile picture to default url`() {
        val user =
            User(
                id = 2L,
                name = "NoPic User",
                email = "nopicture@example.com",
                passwordHash = "secure456",
                profilePictureUrl = null
            )

        val dto = UserMapper.toResponseDTO(user)

        Assertions.assertNotNull(dto.profilePictureUrl)
    }

    @Test
    fun `should map list of Users to list of UserResponseDTOs`() {
        val users =
            listOf(
                User(
                    id = 1L,
                    name = "Alice",
                    email = "alice@example.com",
                    passwordHash = "p1"
                ),
                User(
                    id = 2L,
                    name = "Bob",
                    email = "bob@example.com",
                    passwordHash = "p2"
                )
            )

        val dtos = UserMapper.toResponseDTOList(users)

        Assertions.assertEquals(2, dtos.size)
        Assertions.assertEquals("N/A", dtos[0].profilePictureUrl)
        Assertions.assertEquals("N/A", dtos[1].profilePictureUrl)
    }

    @Test
    fun `should throw when User ID is null`() {
        val user =
            User(
                id = null,
                name = "Invalid",
                email = "invalid@example.com",
                passwordHash = "bad"
            )

        val ex =
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                UserMapper.toResponseDTO(user)
            }

        Assertions.assertEquals("User ID cannot be null", ex.message)
    }
}
