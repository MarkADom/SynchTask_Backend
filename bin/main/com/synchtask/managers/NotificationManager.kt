package com.synchtask.managers

import com.synchtask.entities.User
import com.synchtask.handlers.NotificationHandler
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

/**
 * Central dispatcher for notification handlers.
 * Allows handling notifications dynamically via class reference.
 */
@Service
class NotificationManager(
    private val applicationContext: ApplicationContext
) {

    /**
     * Triggers a notification handler dynamically via its class.
     *
     * @param user The recipient user
     * @param handlerClass The class of the notification handler
     */
    fun handle(user: User, handlerClass: KClass<out NotificationHandler>) {
        val handler = applicationContext.getBean(handlerClass.java)
        handler.handle(user)
    }
}
