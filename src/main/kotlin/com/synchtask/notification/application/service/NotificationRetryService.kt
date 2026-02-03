package com.synchtask.notification.application.service

import com.synchtask.notification.presentation.mapper.NotificationMapper
import com.synchtask.notification.domain.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationRetryService(
    private val notificationRepository: NotificationRepository,
    private val notificationWebSocketService: NotificationWebSocketService
) {

    private val logger = LoggerFactory.getLogger(NotificationRetryService::class.java)

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    fun retryUndeliveredNotifications() {
        val undelivered = notificationRepository.findAllByDeliveredFalse()

        if (undelivered.isEmpty()) {
            logger.debug("No undelivered notifications found.")
            return
        }

        logger.info("Retrying ${undelivered.size} undelivered notifications...")

        undelivered.forEach { notification ->
            try {
                val dto = NotificationMapper.toWebSocketDTO(notification)
                notificationWebSocketService.sendNotification(notification.recipient.email, dto)

                notification.delivered = true
                notificationRepository.save(notification)

                logger.info("Successfully resent notification ${notification.id} to ${notification.recipient.email}")
            } catch (ex: Exception) {
                logger.warn("Retry failed for notification ${notification.id}: ${ex.message}", ex)
            }
        }
    }
}
