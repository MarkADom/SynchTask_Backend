package com.synchtask.activity.domain.repository

import com.synchtask.activity.domain.entity.Activity
import com.synchtask.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityRepository : JpaRepository<Activity, Long> {
    fun findAllByActorOrderByCreatedAtDesc(actor: User): List<Activity>

    fun findAllByReferenceIdOrderByCreatedAtDesc(referenceId: Long): List<Activity>
}
