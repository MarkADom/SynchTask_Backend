package com.synchtask.notification.application.manager

import com.synchtask.notification.application.handler.NotificationHandler
import com.synchtask.user.domain.entity.User
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class NotificationManager(
    private val applicationContext: ApplicationContext
) {

    fun handle(user: User, handlerClass: KClass<out NotificationHandler>) {
        val handler = applicationContext.getBean(handlerClass.java)
        handler.handle(user)
    }
}
