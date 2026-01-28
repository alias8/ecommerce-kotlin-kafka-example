package org.example.ecommerceexamplebackendkotlinkafka.notification

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    }

    init {
        logger.info("NotificationService bean created!")
    }

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_NAME])
    fun handleNotification(message: String) {
        logger.info("NotificationService received: $message")
    }
}