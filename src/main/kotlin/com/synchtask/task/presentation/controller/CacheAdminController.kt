package com.synchtask.task.presentation.controller

import jakarta.persistence.EntityManager
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/cache")
class CacheAdminController(
    private val entityManager: EntityManager
) {

    @PostMapping("/clear")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun clear(): ResponseEntity<String> {
        entityManager.entityManagerFactory.cache.evictAll()
        return ResponseEntity.ok("Cache cleared")
    }
}
