package com.synchtask.notification.domain.repository

import com.synchtask.notification.domain.entity.Notification
import com.synchtask.user.domain.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = ["recipient"])
    fun findByRecipientAndIsReadFalse(user: User): List<Notification>

    @Modifying
    @Transactional
    @Query(
        """
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.recipient = :user
        """
    )
    fun markAllAsReadByRecipient(@Param("user") user: User): Int

    @Modifying
    @Transactional
    fun deleteByRecipient(recipient: User)

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    fun markAsReadById(@Param("id") id: Long): Int

    fun findAllByDeliveredFalse(): List<Notification>
}
