package com.synchtask.services.user

import com.synchtask.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    private val logger = LoggerFactory.getLogger(CustomUserDetailsService::class.java)

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow {
                logger.warn("Authentication failed: user not found for email $username")
                UsernameNotFoundException("User not found: $username")
            }

        if (!user.isActive) {
            logger.warn("Authentication failed: user is disabled (${user.email})")
            throw DisabledException("User is disabled: ${user.email}")
        }

        val authority = "ROLE_${user.role.name}" // e.g., ROLE_OWNER or ROLE_ADMIN
        logger.info("Authenticated user ${user.email} with role $authority")

        return User(
            user.email,
            user.passwordHash,
            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        )
    }
}
