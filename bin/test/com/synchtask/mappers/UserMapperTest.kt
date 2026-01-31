package com.synchtask.mappers

import com.synchtask.entities.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserMapperTest {

    @Test
    fun `should map User to UserResponseDTO correctly`() {
        val user = User(
            id = 1L,
            name = "Jane Doe",
            email = "jane@example.com",
            passwordHash = "secure123",
            profilePictureUrl = "http://example.com/profile.jpg"
        )

        val dto = UserMapper.toResponseDTO(user)

        assertEquals(1L, dto.id)
        assertEquals("Jane Doe", dto.name)
        assertEquals("jane@example.com", dto.email)
        assertEquals("http://example.com/profile.jpg", dto.profilePictureUrl)
    }

    @Test
    fun `should map User with null profile picture to default url`() {
        val user = User(
            id = 2L,
            name = "NoPic User",
            email = "nopicture@example.com",
            passwordHash = "secure456",
            profilePictureUrl = null
        )

        val dto = UserMapper.toResponseDTO(user)

        assertEquals("N/A", dto.profilePictureUrl)
    }

    @Test
    fun `should map list of Users to list of UserResponseDTOs`() {
        val users = listOf(
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

        assertEquals(2, dtos.size)
        assertEquals("Alice", dtos[0].name)
        assertEquals("Bob", dtos[1].name)
    }

    @Test
    fun `should throw when User ID is null`() {
        val user = User(
            id = null,
            name = "Invalid",
            email = "invalid@example.com",
            passwordHash = "bad"
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            UserMapper.toResponseDTO(user)
        }

        assertEquals("User ID cannot be null", ex.message)
    }
}
