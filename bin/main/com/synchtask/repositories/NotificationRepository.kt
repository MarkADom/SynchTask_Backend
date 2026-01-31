package com.synchtask.repositories

import com.synchtask.entities.Notification
import com.synchtask.entities.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * **Notification Repository**
 *
 * Provides database operations for notifications.
 */
@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    /**
    * **Finds unread notifications for a specific user.**
    */
    @EntityGraph(attributePaths = ["recipient"])
    fun findByRecipientAndIsReadFalse(user: User): List<Notification>

    /**
     * **Marks all notifications as read for a specific user.**
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :user")
    fun markAllAsReadByRecipient(@Param("user") user: User): Int

    /**
     * **Deletes all notifications for a user.**
     */
    @Modifying
    @Transactional
    fun deleteByRecipient(recipient: User)

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    fun markAsReadById(@Param("id") id: Long): Int

    /**
     * **Finds all notifications that were never delivered via WebSocket.**
     */
    fun findAllByDeliveredFalse(): List<Notification>
}
